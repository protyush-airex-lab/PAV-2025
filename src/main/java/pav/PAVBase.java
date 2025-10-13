/* This file has the prescribed format for your file output.
 * Though you can call the functions in this class, you will not need to modify this file to implement your analysis.
 * So, do not modify this file unless you know exactly what you are doing.
 */

package pav;

import java.util.*;

public class PAVBase {

	protected static String getProgramPointName(int st1) {
		String name1 = "in" + String.format("%02d", st1);
		return name1;
	}

	public static class ResultTuple {
		public final String m;
		public final String p;
		public final String v;
		public final List<String> pV;

		public ResultTuple(String method, String prgpoint, String varname, List<String> pointerValues) {
			this.m = method;
			this.p = prgpoint;
			this.v = varname;
			this.pV = pointerValues;
		}
	}

	protected static String formatOutputLine(ResultTuple tup, String prefix) {
		String line = tup.m + ": " + tup.p + ": " + tup.v + ": " + "{";
		List<String> pointerValues = tup.pV;
		for(String pointers: pointerValues) {
			line += pointers+", ";
		}
		line= line+"}";
		return (prefix + line);
	}
	protected static String fmtOutputLine(ResultTuple tup) {
		return formatOutputLine(tup, "");
	}

	protected static String[] formatOutputData(Set<ResultTuple> data, String prefix) {

		String[] outputlines = new String[ data.size() ];

		int i = 0;
		for (ResultTuple tup : data) {
			outputlines[i] = formatOutputLine(tup, prefix);
			i++;
		}

		Arrays.sort(outputlines);
		return outputlines;
	}

	protected static String[] formatOutputData(Set<ResultTuple> data) {
		return formatOutputData(data, "");
	}
}