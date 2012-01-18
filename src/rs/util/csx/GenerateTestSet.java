package rs.util.csx;

import java.math.*;
import java.io.*;

import cc.mallet.util.Maths;

import gnu.trove.map.hash.TObjectIntHashMap;

public class GenerateTestSet {
	public String[] ids;
	TObjectIntHashMap<String> trainIdHash;
	TObjectIntHashMap<String> testIdHash;
	
	public TObjectIntHashMap<String> importIds(String idFile) throws IOException {
		TObjectIntHashMap<String> idHash = new TObjectIntHashMap<String>();
		BufferedReader reader = new BufferedReader(new FileReader(idFile));
		String str;
		int total = 0;
		while( (str=reader.readLine()) != null) {
			str = str.trim();
			idHash.put(str, total);
			total++;
		}
		reader.close();
		return idHash;
	}

	/**
	 * Generate the target solution file from randomly selected test ids. 
	 */
	public void generateTargetFilef() {
		
	}
}
