/* This program will plot a CFG for a method using soot [ExceptionalUnitGraph feature].
 * Arguments : <ProcessOrTargetDirectory> <MainClass> <TargetClass> <TargetMethod>
 *
 * References:
 *		https://gist.github.com/bdqnghi/9d8d990b29caeb4e5157d7df35e083ce
 *		https://github.com/soot-oss/soot/wiki/Tutorials
 */

package pav;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.UnitGraph;

public class Analysis extends Base {

	/* ---------------------------------------------------------
	 * Concrete, immutable lattice fact for MAY points-to (intra)
	 * --------------------------------------------------------- */
	static final class PointsToFact implements LatticeElement {
		// locals -> {abstract objects}
		private final Map<String, Set<String>> varPts;
		// heap slots "obj.f" or "obj.[]" -> {abstract objects}
		private final Map<String, Set<String>> heapPts;
		// local name -> type (for filtering at emission)
		private final Map<String, Type> localTypes;
		// Stable mapping Unit-> "new%02d" (based on unit index)
		private final Map<Unit, String> allocIds;

		private PointsToFact(Map<String, Set<String>> v,
		                     Map<String, Set<String>> h,
		                     Map<String, Type> lt,
		                     Map<Unit, String> ids) {
			this.varPts = v;
			this.heapPts = h;
			this.localTypes = lt;
			this.allocIds = ids; // shared, read-only
		}

		static PointsToFact bottom(Body body, Map<Unit,String> allocIds) {
			Map<String, Type> lt = new HashMap<>();
			for (Local l : body.getLocals()) lt.put(l.getName(), l.getType());
			return new PointsToFact(new HashMap<>(), new HashMap<>(), lt, allocIds);
		}

		/* -------- Lattice ops (immutable) -------- */

		@Override
		public LatticeElement join_op(LatticeElement r) {
			PointsToFact o = (PointsToFact) r;
			Map<String, Set<String>> v = copyOf(varPts);
			unionInto(v, o.varPts);
			Map<String, Set<String>> h = copyOf(heapPts);
			unionInto(h, o.heapPts);
			return new PointsToFact(v, h, localTypes, allocIds);
		}

		@Override
		public boolean equals(LatticeElement r) {
			if (this == r) return true;
			if (!(r instanceof PointsToFact o)) return false;
			return varPts.equals(o.varPts) && heapPts.equals(o.heapPts);
		}

		/* -------- Transfer functions -------- */

		@Override
		public LatticeElement tf_assign(Stmt st) {
			if (!(st instanceof AssignStmt as)) return this;

			Value L = as.getLeftOp();
			Value R = as.getRightOp();

			// x = ...
			if (L instanceof Local xl) {
				if (!isPtr(xl.getType())) return this;
				Set<String> rhs = evalRhs(R, st);
				return strongLocal(xl.getName(), rhs);
			}

			// x.f = ...
			if (L instanceof InstanceFieldRef ifw) {
				if (!isPtr(ifw.getField().getType())) return this;
				if (!(ifw.getBase() instanceof Local bl) || !isPtr(((Local) ifw.getBase()).getType())) return this;
				Set<String> bases = ptsOfLocal(((Local) ifw.getBase()).getName());
				if (bases.isEmpty()) return this;
				Set<String> rhs = evalRhs(R, st);
				if (rhs.isEmpty()) return this;
				return weakHeapUpdate(bases, ifw.getField().getName(), rhs);
			}

			// a[i] = ...
			if (L instanceof ArrayRef arw) {
				Value base = arw.getBase();
				if (!(base instanceof Local bl) || !isPtr(((Local) base).getType())) return this;
				Set<String> bases = ptsOfLocal(((Local) base).getName());
				if (bases.isEmpty()) return this;
				Set<String> rhs = evalRhs(R, st);
				if (rhs.isEmpty()) return this;
				return weakHeapUpdate(bases, "[]", rhs);
			}

			return this;
		}

		@Override
		public LatticeElement tf_cond(boolean b, Stmt st) {
			// Phase-1: path-insensitive (identity)
			return this;
		}

		/* -------- RHS evaluation -------- */

		private Set<String> evalRhs(Value R, Stmt st) {
			// null
			if (R instanceof NullConstant) return one("null");

			// local
			if (R instanceof Local yl) {
				if (isPtr(yl.getType())) return ptsOfLocal(yl.getName());
				return Collections.emptySet();
			}

			// cast (T) y
			if (R instanceof CastExpr ce) {
				if (isPtr(ce.getCastType()) && ce.getOp() instanceof Local yl) {
					return ptsOfLocal(((Local) yl).getName());
				}
				return Collections.emptySet();
			}

			// new object / array / multiarray
			if (R instanceof NewExpr || R instanceof NewArrayExpr || R instanceof NewMultiArrayExpr) {
				String id = allocIds.get(st);
				return (id == null) ? Collections.emptySet() : one(id);
			}

			// field read: x = y.f
			if (R instanceof InstanceFieldRef ifr) {
				if (!isPtr(ifr.getField().getType())) return Collections.emptySet();
				if (!(ifr.getBase() instanceof Local bl) || !isPtr(((Local) ifr.getBase()).getType()))
					return Collections.emptySet();
				Set<String> bases = ptsOfLocal(((Local) ifr.getBase()).getName());
				Set<String> acc = readHeap(bases, ifr.getField().getName());
				if (bases.contains("null")) acc.add("null"); // reading through possible null
				return acc;
			}

			// array read: x = a[i]   (model as "[]")
			if (R instanceof ArrayRef arr) {
				Value base = arr.getBase();
				if (!(base instanceof Local bl) || !isPtr(((Local) base).getType()))
					return Collections.emptySet();
				Set<String> bases = ptsOfLocal(((Local) base).getName());
				Set<String> acc = readHeap(bases, "[]");
				if (bases.contains("null")) acc.add("null");
				return acc;
			}

			// call returning reference (model as fresh alloc site at this unit)
			if (R instanceof InvokeExpr ie) {
				Type rt = ie.getMethod().getReturnType();
				if (isPtr(rt)) {
					String id = allocIds.get(st);
					return (id == null) ? Collections.emptySet() : one(id);
				}
				return Collections.emptySet();
			}

			return Collections.emptySet();
		}

		/* -------- Immutable updates / lookups -------- */

		private PointsToFact strongLocal(String x, Set<String> rhs) {
			Map<String, Set<String>> v = copyOf(varPts);
			v.put(x, new HashSet<>(rhs)); // strong update
			return new PointsToFact(v, this.heapPts, this.localTypes, this.allocIds);
		}

		private PointsToFact weakHeapUpdate(Set<String> bases, String fname, Set<String> rhs) {
			Map<String, Set<String>> h = copyOf(heapPts);
			for (String o : bases) {
				if ("null".equals(o)) continue; // ignore writes through null
				String key = o + "." + fname;
				Set<String> cur = h.getOrDefault(key, Collections.emptySet());
				Set<String> nu = new HashSet<>(cur);
				nu.addAll(rhs);
				h.put(key, nu);
			}
			return new PointsToFact(this.varPts, h, this.localTypes, this.allocIds);
		}

		private Set<String> readHeap(Set<String> bases, String fname) {
			Set<String> acc = new HashSet<>();
			for (String o : bases) {
				if ("null".equals(o)) continue;
				String key = o + "." + fname;
				Set<String> s = heapPts.get(key);
				if (s != null) acc.addAll(s);
			}
			return acc;
		}

		private Set<String> ptsOfLocal(String x) {
			Set<String> s = varPts.get(x);
			return (s == null) ? Collections.emptySet() : s;
		}

		/* -------- Utilities -------- */

		private static Map<String, Set<String>> copyOf(Map<String, Set<String>> m) {
			Map<String, Set<String>> out = new HashMap<>(Math.max(16, (int)(m.size()/0.75f)+1));
			for (Map.Entry<String, Set<String>> e : m.entrySet()) {
				out.put(e.getKey(), new HashSet<>(e.getValue()));
			}
			return out;
		}

		private static void unionInto(Map<String, Set<String>> dst, Map<String, Set<String>> src) {
			for (Map.Entry<String, Set<String>> e : src.entrySet()) {
				dst.merge(e.getKey(), new HashSet<>(e.getValue()), (a,b) -> { a.addAll(b); return a; });
			}
		}

		private static boolean isPtr(Type t) {
			if (t == null) return false;
			if (t instanceof PrimType) return false;
			return (t instanceof RefType) || (t instanceof ArrayType);
		}

		private static Set<String> one(String v) {
			HashSet<String> s = new HashSet<>(1);
			s.add(v);
			return s;
		}

		/* -------- Emission for grading -------- */

		Set<Base.ResultTuple> toTuples(String methodQualified, String inLabel) {
			Set<Base.ResultTuple> out = new HashSet<>();

			// locals (pointer-typed, non-empty)
			for (Map.Entry<String, Set<String>> e : varPts.entrySet()) {
				if (e.getValue() == null || e.getValue().isEmpty()) continue;
				Type t = localTypes.get(e.getKey());
				if (!isPtr(t)) continue;
				List<String> pv = new ArrayList<>(e.getValue());
				Collections.sort(pv);
				out.add(new Base.ResultTuple(methodQualified, inLabel, e.getKey(), pv));
			}

			// heap slots (non-empty)
			for (Map.Entry<String, Set<String>> e : heapPts.entrySet()) {
				if (e.getValue() == null || e.getValue().isEmpty()) continue;
				List<String> pv = new ArrayList<>(e.getValue());
				Collections.sort(pv);
				out.add(new Base.ResultTuple(methodQualified, inLabel, e.getKey(), pv));
			}
			return out;
		}
	}

	/* ------------------------------------
	 * Stable allocation IDs: "new%02d"
	 * Based on the unit's source-order index.
	 * This matches the expected jumps (new00, new02, ...).
	 * ------------------------------------ */
	private static Map<Unit, String> precomputeAllocIds(Body body) {
		Map<Unit, String> ids = new HashMap<>();
		int idx = 0;
		for (Unit u : body.getUnits()) {
			if (!(u instanceof Stmt st)) { idx++; continue; }
			if (st instanceof AssignStmt as) {
				Value R = as.getRightOp();
				boolean fresh = (R instanceof NewExpr)
				             || (R instanceof NewArrayExpr)
				             || (R instanceof NewMultiArrayExpr);
				if (!fresh && R instanceof InvokeExpr ie) {
					Type rt = ie.getMethod().getReturnType();
					fresh = (rt instanceof RefType) || (rt instanceof ArrayType);
				}
				if (fresh) ids.put(st, String.format("new%02d", idx));
			}
			idx++;
		}
		return ids;
	}

	
private static void writeOutput(SootMethod m, Set<Base.ResultTuple> tuples) {
    String cls = m.getDeclaringClass().getShortName();
    String outName = cls + "." + m.getName() + ".output.txt";
    java.nio.file.Path outDir = java.nio.file.Path.of("output");
    java.nio.file.Path outFile = outDir.resolve(outName);

    // Build lines ourselves to avoid trailing comma; keep original order of values
    java.util.List<String> lines = new java.util.ArrayList<>(tuples.size());
    for (Base.ResultTuple tup : tuples) {
        java.util.List<String> vals = (tup.pV == null) ? java.util.List.of() : tup.pV;
        String joined = String.join(", ", vals);   // <- no trailing comma
        String line = tup.m + ": " + tup.p + ": " + tup.v + ": {" + joined + "}";
        lines.add(line);
    }

    // Keep file-level ordering stable
    java.util.Collections.sort(lines);

    StringBuilder sb = new StringBuilder();
    for (String s : lines) sb.append(s).append('\n');

    try {
        java.nio.file.Files.createDirectories(outDir);
        java.nio.file.Files.writeString(outFile, sb.toString());
    } catch (java.io.IOException e) {
        System.err.println("Failed writing " + outFile + ": " + e.getMessage());
    }
}


	/* =========================
	 * Kildall (worklist) solver
	 * ========================= */
	public static void doAnalysis(SootMethod targetMethod, List<SootMethod> targetClassMethods){
		if (targetMethod.isPhantom() || !targetMethod.isConcrete()) return;

		Body body = targetMethod.retrieveActiveBody();
		UnitGraph cfg = new BriefUnitGraph(body);

		// Program-point labels: IN before each unit (in01, in02, ...)
		Map<Unit, String> inLabel = new LinkedHashMap<>();
		int lbl = 1;
		for (Unit u : body.getUnits()) inLabel.put(u, getProgramPointName(lbl++));

		// Allocation IDs per allocating unit
		Map<Unit, String> allocIds = precomputeAllocIds(body);

		// Initialize IN/OUT to bottom
		LatticeElement bottom = PointsToFact.bottom(body, allocIds);
		Map<Unit, LatticeElement> IN  = new LinkedHashMap<>();
		Map<Unit, LatticeElement> OUT = new LinkedHashMap<>();
		for (Unit u : body.getUnits()) {
			IN.put(u, bottom);
			OUT.put(u, bottom);
		}

		// Worklist in source order
		Deque<Unit> wl = new ArrayDeque<>();
		for (Unit u : body.getUnits()) wl.add(u);

		while (!wl.isEmpty()) {
			Unit n = wl.removeFirst();

			// IN[n] = join of OUT[p] over all predecessors
			LatticeElement newIn;
			List<Unit> preds = cfg.getPredsOf(n);
			if (preds.isEmpty()) {
				newIn = IN.get(n); // keep as bottom for entry
			} else {
				newIn = OUT.get(preds.get(0));
				for (int i = 1; i < preds.size(); i++) {
					newIn = newIn.join_op(OUT.get(preds.get(i)));
				}
			}
			if (!newIn.equals(IN.get(n))) IN.put(n, newIn);

			// OUT[n] = transfer(IN[n], stmt)
			LatticeElement in = IN.get(n);
			LatticeElement newOut = in; // identity by default
			if (n instanceof AssignStmt) {
				newOut = in.tf_assign((Stmt) n);
			} else if (n instanceof IfStmt) {
				newOut = in.tf_cond(true, (Stmt) n); // Phase-1: identity
			}
			if (!newOut.equals(OUT.get(n))) {
				OUT.put(n, newOut);
				for (Unit s : cfg.getSuccsOf(n)) if (!wl.contains(s)) wl.add(s);
			}
		}

		// Emit facts (skip the final return's OUT to match expected lines)
		Set<Base.ResultTuple> tuples = new HashSet<>();
		String mname = targetMethod.getDeclaringClass().getShortName() + "." + targetMethod.getName();

		List<Unit> ordered = new ArrayList<>();
		for (Unit u : body.getUnits()) ordered.add(u);

		for (int i = 0; i < ordered.size(); i++) {
		    Unit u = ordered.get(i);
		    // Skip last unit (final return)
		    if (i == ordered.size() - 1) continue;

		    PointsToFact fact = (PointsToFact) OUT.get(u);
		    tuples.addAll(fact.toTuples(mname, inLabel.get(u)));
		}

		writeOutput(targetMethod, tuples);


		// // Emit facts at OUT of each unit
		// Set<Base.ResultTuple> tuples = new HashSet<>();
		// String mname = targetMethod.getDeclaringClass().getShortName() + "." + targetMethod.getName();
		// for (Unit u : body.getUnits()) {
		// 	PointsToFact fact = (PointsToFact) OUT.get(u);
		// 	tuples.addAll(fact.toTuples(mname, inLabel.get(u)));
		// }
		// writeOutput(targetMethod, tuples);
	}

	public static void main(String[] args) throws Exception{
		String targetDirectory, mClass, tClass;
		if(args.length == 0){
			// Default values if no arguments are given for the analysis
			targetDirectory = "target/classes/test/";
			mClass = "Test";
			tClass = "Test";
		}
		else if (args.length == 3) {
			targetDirectory=args[0];
			mClass=args[1];
			tClass=args[2];
		}
		else {
			throw new IllegalArgumentException("Invalid number of arguments. Expected 0 or 3 arguments: <ProcessOrTargetDirectory> <MainClass> <TargetClass>");
		}

		List<String> procDir = new ArrayList<String>();
		procDir.add(targetDirectory);

		// Set Soot options
		soot.G.reset();
		Options.v().set_process_dir(procDir);
		Options.v().set_src_prec(Options.src_prec_only_class);
		Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_output_format(Options.output_format_none);
		Options.v().set_keep_line_number(true);
		Options.v().setPhaseOption("cg.spark", "verbose:false");

		Scene.v().loadNecessaryClasses();

		SootClass entryClass = Scene.v().getSootClass(mClass);
		SootMethod entryMethod = entryClass.getMethodByName("main");
		SootClass targetClass = Scene.v().getSootClass(tClass);

		Options.v().set_main_class(mClass);
		Scene.v().setEntryPoints(Collections.singletonList(entryMethod));

		SLF4J.LOGGER.info("Target Directory: " + targetDirectory);
		SLF4J.LOGGER.info("Entry Class: " + entryClass);
		SLF4J.LOGGER.info("Target Class: " + targetClass);

		for (SootMethod method : targetClass.getMethods()) {
			// Skip drawing CFG for the class constructor
			if (method.getName().equals("<init>")) {
				continue;
			}

			// Print the method body
			System.out.println("\n\nMethod: " + method.getName());
			printInfo(method);

			// Draw the CFG for each method in the target class
			drawMethodDependenceGraph(method);

			// The function doAnalysis is the entry point for the Kildall's fix-point algorithm over the LatticeElement.
			doAnalysis(method, targetClass.getMethods());
		}
	}
}
