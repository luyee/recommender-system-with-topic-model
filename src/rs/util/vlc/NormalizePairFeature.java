package rs.util.vlc;

import java.io.*;
import java.text.*;
import java.util.Arrays;

import gnu.trove.map.hash.*;
import gnu.trove.list.array.*;

public class NormalizePairFeature {
	TIntIntHashMap idHash; // idsHash[ids] = num, num will be the index in the matrix
	final int FREQ_THRESH = 0;
	final double RATE_THRESH = 0.05;
	int vSize;
	
//	int[][] pairs;
//	double[][] features;
	TObjectDoubleHashMap<String> features;
	int[] totalFreq;

	public void outputFeature(String file) throws IOException {
		if (null == features ) {
			System.err.println("Features are not calculated yet.");
			return;
		}
		BufferedWriter writer = new BufferedWriter(
				new FileWriter(file));
	
		int[] videoIds = idHash.keys();
		for(int i=0; i<videoIds.length; i++) {
			for (int j=0; j<i; j++) {
				int v1Idx = idHash.get(videoIds[i]);
				int v2Idx = idHash.get(videoIds[j]);
				StringBuilder key = new StringBuilder();
//				key.append(v1Idx);
				key.append(videoIds[i]);
				key.append(',');
//				key.append(v2Idx);
				key.append(videoIds[j]);
				
				if (features.contains(key.toString())) {			
					writer.write(featureLine(videoIds[i], videoIds[j], features.get(key.toString())));
					writer.newLine();
				}
			}
		}
		writer.flush();
		writer.close();
		
	}
	
	private String featureLine(int v1, int v2, double sim) {
		StringBuilder sb = new StringBuilder();
		sb.append(v1);
		sb.append(',');
		sb.append(v2);
		sb.append(',');
		DecimalFormat form = new DecimalFormat("#.########");
		String value = form.format(sim);
		sb.append(value);
		return sb.toString();
	}
	
	public void readPairsData(String idFile, String pairFile) throws IOException {
		initIds(idFile);
		readPairsData(pairFile);
	}
	
	/**
	 * Read pair frequencies into matrix of pairs.
	 * @param pairFile
	 * @throws IOException
	 */
	private void readPairsData(String pairFile) throws IOException {
		BufferedReader reader = new BufferedReader(
				new FileReader(pairFile));
		String s;
		Arrays.fill(totalFreq, 0);
		
		while( (s=reader.readLine()) != null) {
			if (s.trim().startsWith("#")) continue;
			String[] fields = s.split("\\s*,\\s*");
			if (fields.length != 3) {
				System.out.println("Fields length less than 3.");
				continue;
			}
			int id1, id2, freq;
			id1 = Integer.parseInt(fields[0]);
			id2 = Integer.parseInt(fields[1]);
			freq = Integer.parseInt(fields[2]);
//			if (freq < FREQ_THRESH) continue;
			
			int id1Idx = idHash.get(id1); 
			int id2Idx = idHash.get(id2);
//			pairs[id1Idx][id2Idx] = pairs[id1Idx][id2Idx] = freq;
			totalFreq[id1Idx] += freq;
			totalFreq[id2Idx] += freq;
		}
		reader.close();
	}
	
	/**
	 * Since we deem the similarity between two videos are not directional, 
	 * we calculate the similarity as freq/((total[v1]+total[v2])/2)
	 */
	public void normalizeFeature(String pairFile) throws IOException{
		BufferedReader reader = new BufferedReader(
				new FileReader(pairFile));
		String s;
		int line = 0;
		while( (s=reader.readLine()) != null) {
			
			if (s.trim().startsWith("#")) continue;
			String[] fields = s.split(",");
			if (fields.length != 3) continue;
			
			int id1, id2, freq;
			id1 = Integer.parseInt(fields[0]);
			id2 = Integer.parseInt(fields[1]);
			String key1 = fields[0].trim() + "," + fields[1].trim();
			String key2 = fields[1].trim() + "," + fields[0].trim();
			freq = Integer.parseInt(fields[2]);
			double sim1 = (double)freq/(double)totalFreq[idHash.get(id1)];
			double sim2 = (double)freq/(double)totalFreq[idHash.get(id2)];
			
			if ( freq < FREQ_THRESH || (sim1 < RATE_THRESH && sim2 < RATE_THRESH) ) continue;
			double sim = freq/((double)totalFreq[idHash.get(id1)] + (double)totalFreq[idHash.get(id2)]);
			features.put(key1, sim);
			features.put(key2, sim);
			line++;
		}
		reader.close();
		System.out.println("Totla lines: " + line);
//		for(int i=0; i<vSize; i++) {
//			for(int j=0; j<i; j++) {
//				if (pairs[i][j] > FREQ_THRESH) {
//					features[i][j] = features[j][i] = 2 * pairs[i][j]/(totalFreq[i] + totalFreq[j]);
//				}
//			}
//		}
	}
	
	/**
	 *  Read id list into memory and put them into a hash map to be used later.
	 * @param idFile	File of id list, each line contains one id.
	 * @throws IOException
	 */
	private void initIds(String idFile) throws IOException {
		BufferedReader reader = new BufferedReader(
					new FileReader(idFile));
		idHash = new TIntIntHashMap();
		int idPos = 0;
		String s;
		while( (s = reader.readLine()) != null) {
			if (s.startsWith("#")) {
				continue;
			}
			int id = Integer.parseInt(s);
			idHash.put(id, idPos);
			idPos++;
		}
		reader.close();
		vSize = idHash.size();
		System.out.println(vSize);
		totalFreq = new int[vSize];
//		pairs = new int[vSize][vSize];
//		features = new double[vSize][vSize];
		features = new TObjectDoubleHashMap<String>();
	}

	
	/** Test 
	 * @throws IOException */
	public static void main(String[] args) throws IOException {
		NormalizePairFeature f = new NormalizePairFeature();
		String pairFile = "dataset/pairs.csv";
		f.readPairsData("dataset/lecture_ids.csv", "dataset/pairs.csv");
		f.normalizeFeature(pairFile);
		f.outputFeature("dataset/vlc/sim5p.csv");
	}
}
