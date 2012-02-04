/*
 * This class will split links into two parts, one for training, one 
 * for evaluation. We will try to randomly select 1/5 size of the whole links.
 */
package rs.util;

import java.io.*;

import gnu.trove.TIntIntHashMap;
import java.util.*;

public class GenerateLinkFolds {
	public static void generate(String sourceFile, String trainFile, String testFile) throws IOException {
		TIntIntHashMap samples = new TIntIntHashMap();
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		String line;
		ArrayList<String> lines = new ArrayList<String> ();
		while( (line = reader.readLine()) != null) {
			lines.add(line);
		}
		reader.close();
		
		int sampleSize = (int) (lines.size()/5);
		Random r = new Random();
		for(int i=0; i<sampleSize; i++) {
			int ln = r.nextInt(lines.size());
			while(samples.containsKey(ln)) {
				ln = r.nextInt(lines.size());
			}
			samples.put(ln, 1);
		}
		
		BufferedWriter tWriter = new BufferedWriter(new FileWriter(trainFile));
		BufferedWriter sWriter = new BufferedWriter(new FileWriter(testFile));
		for(int i=0; i<lines.size(); i++) {
			if(samples.contains(i)) {
				sWriter.write(lines.get(i));
				sWriter.newLine();
			} else {
				tWriter.write(lines.get(i));
				tWriter.newLine();
			}
		}
		
		sWriter.flush();
		sWriter.close();
		tWriter.flush();
		tWriter.close();
	}
	
	public static void main(String[] args) throws IOException {
		generate("dataset/cora/links.txt", "dataset/cora/links.train.txt", "dataset/cora/links.test.txt");
	}
}
