package rs.topics.link.likelihood;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.*;

public class Util {
	public static ArrayList<Edge> importEdges(String testFile, TObjectIntHashMap<String> idHash) throws IOException {
		ArrayList<Edge> samples = new ArrayList<Edge>();
		BufferedReader reader = new BufferedReader(
				new FileReader(testFile));
		String s;
		while( (s=reader.readLine()) != null) {
			if (s.trim().startsWith("#")) continue;
			String[] fields = s.split("\\s*,\\s*");
			if (fields.length != 3) continue;
			int doc1, doc2;
			double sim;
	
			doc1 = idHash.get(fields[0].trim());
			doc2 = idHash.get(fields[1].trim());
			sim = Double.parseDouble(fields[2]);
			
			samples.add(new Edge(doc1, doc2, sim));
		}
		return samples;
	}
	
	public static double sigmoid(double x) {
		double result = 1 + Math.exp(0 - x);
		result = 1/ result;
		return result;
	}
}
