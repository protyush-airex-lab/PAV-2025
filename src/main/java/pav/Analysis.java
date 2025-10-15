/* This program will plot a CFG for a method using soot [ExceptionalUnitGraph feature].
 * Arguments : <ProcessOrTargetDirectory> <MainClass> <TargetClass> <TargetMethod>
 *
 * References:
 *		https://gist.github.com/bdqnghi/9d8d990b29caeb4e5157d7df35e083ce
 *		https://github.com/soot-oss/soot/wiki/Tutorials
 */

package pav;

import java.util.*;
import soot.options.Options;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class Analysis extends Base {
	public static void doAnalysis(SootMethod targetMethod, List<SootMethod> targetClassMethods){
		// Implement your analysis here
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