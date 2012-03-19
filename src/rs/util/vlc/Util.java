package rs.util.vlc;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;

public class Util {
	public static TObjectIntHashMap getIds(String idFile) throws IOException {
		BufferedReader reader = new BufferedReader(
				new FileReader(idFile));
		TObjectIntHashMap<String> idHash = new TObjectIntHashMap<String>();
		
		int idPos = 0;
		String s;
		while( (s = reader.readLine()) != null) {
			if (s.startsWith("#")) {
				continue;
			}
			idHash.put(s.trim(), idPos);
			idPos++;
		}
		reader.close();
		
		return idHash;
	}
	
	public static double cosineProduct(double[] v1, double[] v2) {
		double sim = 0;
		for(int i=0; i<v1.length; i++) {
			sim += v1[i] * v2[i];
		}
		
		return sim/(normalizeVector(v1) * normalizeVector(v2));
	}
	
	public static double normalizeVector(double[] v) {
		double sum = 0;
		for(int i=0; i<v.length; i++) {
			sum += v[i] * v[i];
		}
		return Math.sqrt(sum);
	}
	
	public static double normalizeVector(int[] v) {
		double sum = 0;
		for(int i=0; i<v.length; i++) {
			sum += v[i] * v[i];
		}
		return Math.sqrt(sum);
	}
	
	
	/**
	 * Rescale a vector by dividing every factor with the sum of the vector. 
	 * @param v
	 * @return
	 */
	public static double[] reWeightVector(double[] v) {
		double sum = 0;
		for (int i=0; i<v.length; i++) {
			sum += v[i];
		}
		if(sum == 0) return v;
		double[] result = v.clone();
		for(int i=0; i<v.length; i++) {
			result[i] = v[i] / sum;
		}
		return result;
	}
	
	/**
	 * eta's length should be one longer than v1 and v2.
	 * @param v1
	 * @param v2
	 * @param eta
	 * @return
	 */
	public static double weightedProduct(double[] v1, double[] v2, double[] eta) {
		double result = eta[0];
		for(int i=0; i<v1.length; i++) {
			result += v1[i] * v2[i] * eta[i+1];
		}
		return result;
	}
}
