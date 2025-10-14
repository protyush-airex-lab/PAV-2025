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

	public static void main(String[] args) {
		String targetDirectory, tClass;
		if(args.length == 0){
			// Default values
			targetDirectory = "target/classes/test/";
			tClass = "Test";
		}
		else {
			targetDirectory=args[0];
			tClass=args[1];
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

		SootClass targetClass = Scene.v().getSootClass(tClass);
		SootMethod entryMethod = targetClass.getMethodByName("main");

		Options.v().set_main_class(tClass);
		Scene.v().setEntryPoints(Collections.singletonList(entryMethod));

		System.out.println("Target Class: " + targetClass);
		
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