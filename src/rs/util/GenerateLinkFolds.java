package rs.util;
/**
 * This class will split links into two parts, one for training, one 
 * for evaluation. We will try to randomly select 1/5 size of the whole links.
 */

import java.io.*;

import gnu.trove.TIntIntHashMap;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.*;

import cc.mallet.types.InstanceList;

import rs.types.PairData;
import rs.types.PairedInfo;
import rs.types.VideoRank;

public class GenerateLinkFolds {

	public final static int FREQ = 2;
	
	/**
	 * This method will randomly divide training data set into specified folds. 
	 * Different from the link folds method, we randomly extract some ids from 
	 * training data set as a test set, and move all connection between training 
	 * and test set as the target link set. Evaluation will be carried out over 
	 * this target link set.  
	 * 
	 * @param srcFile
	 * @param linkSrcFile
	 * @param targetFolder
	 */
	public void generateDataFolds(String srcFile, String linkSrcFile, 
								String targetFolder, int fold) {
		PairData pairs = new PairData();
		try {
			pairs.initFromFile(srcFile, linkSrcFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (pairs == null) return;
		InstanceList documents = pairs.documents;

		Integer[] pIdx = new Integer[documents.size()];
		for(int ii=0; ii<pIdx.length; ii++) {
			pIdx[ii] = ii;
		}
		Collections.shuffle(Arrays.asList(pIdx));
		int[] idx = new int[pIdx.length];
		for (int ii=0; ii<idx.length; ii++)
			idx[ii] = pIdx[ii];
		
		int sampleSize = (int) (idx.length / fold);
		for (int fi=0; fi<fold; fi++) {
			int start = fi*sampleSize;
			int end = Math.min(start+sampleSize, idx.length);
			if (fi == (fold-1)) 
				end = idx.length;
			TIntIntHashMap testIds = new TIntIntHashMap();
			for (int di=start; di<end; di++) {
				testIds.put(idx[di], 1);
			}
			
			splitDocuments(idx, fi, testIds, documents, targetFolder);
			try {
				splitPairs(idx, fi, testIds, documents, pairs, targetFolder);
				splitTargets(idx, fi, testIds, documents, pairs, targetFolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		
	}
	
	/**
	 * Split the instance list into specified folds according to the shuffled order. 
	 * @param idx
	 * @param fold
	 * @param documents
	 * @param outFolder
	 */
	private void splitDocuments(int[] idx, int fi, TIntIntHashMap testIds,
				InstanceList documents, String outFolder) {		
		InstanceList training = documents.cloneEmpty();
		InstanceList test = documents.cloneEmpty();
		
		for (int di=0; di<documents.size(); di++) {
			int targetDoc = idx[di];
			if (testIds.containsKey(targetDoc)) 
				test.add(documents.get(targetDoc));
			else 
				training.add(documents.get(targetDoc));
		}
		
		InstanceList allData = training.shallowClone();
		for (int di=0; di<test.size(); di++) {
			allData.add(test.get(di));
		}
		
		String trainMallet = outFolder + "/training." + fi + ".mallet";
		String testMallet = outFolder + "/test." + fi + ".mallet";
		String allMallet = outFolder + "/all." + fi + "." + training.size() + ".mallet";
		training.save(new File(trainMallet));
		test.save(new File(testMallet));
		allData.save(new File(allMallet));
	}
	
	/**
	 * Since in PairData we have pairIdHash, which maps a string of pair id into an 
	 * interger. This id string is composed of two documents id connected with a "X". 
	 * This method will simply scan over every pair and check if the id contains test 
	 * document id, if not, we will output it, otherwise we will simply skip it. 
	 * 
	 * @param idx
	 * @param fold
	 * @param Documents
	 * @param pairs
	 * @param outFolder
	 * @throws IOException 
	 */
	private void splitPairs(int[] idx, int fi, TIntIntHashMap testIds, InstanceList documents, 
						PairData pairs, String outFolder) throws IOException {
		String pairFile = outFolder + "/trainingPairs." + fi + ".csv";
		BufferedWriter writer = new BufferedWriter(new FileWriter(pairFile));

		TObjectIntIterator<String> iterator = pairs.pairIdHash.iterator();
		while(iterator.hasNext()) {
			iterator.advance();
			String key = iterator.key();
			String[] ids = key.split(pairs.BY);
			int v1 = Integer.parseInt(ids[0]);
			int v2 = Integer.parseInt(ids[1]);
			if (testIds.containsKey(v1) || testIds.containsKey(v2)) 
				continue;
			
			double simVal = pairs.getPairedSim(v1, v2);
			if(simVal < FREQ) continue;
			String docId1 = (String) documents.get(v1).getName();
			String docId2 = (String) documents.get(v2).getName();
			writer.write(docId1 + "," + docId2 + "," + simVal);
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}
	
	/**
	 * This method will output the pairs between training set and test set, 
	 * and output it in the format of target and query id.
	 * @param idx
	 * @param fold
	 * @param documents
	 * @param pairs
	 * @param outFolder
	 * @throws IOException
	 */
	private void splitTargets(int[] idx, int fi, TIntIntHashMap testIds, InstanceList documents, 
					PairData pairs, String outFolder) throws IOException {
		String qIdFile = outFolder + "/query." + fi + ".csv";
		String targetFile = outFolder + "/target." + fi + ".csv";
		BufferedWriter qWriter = new BufferedWriter(new FileWriter(qIdFile));
		BufferedWriter tWriter = new BufferedWriter(new FileWriter(targetFile));
		
		for (int di=0; di<documents.size(); di++) {
			if(testIds.containsKey(di)) continue;
			String qIdString = (String)documents.get(di).getName();
			PairedInfo qPairs = pairs.links.get(di);
			
			if (qPairs.isEmpty()) continue;
			
			int[] pIds = qPairs.getPairedIdsArray();
			double[] sims = qPairs.getPairedSimArray();
			ArrayList<VideoRank> videoList = new ArrayList<VideoRank>();
			for (int pi=0; pi<pIds.length; pi++) {
				if (!testIds.containsKey(pIds[pi])) continue;
				videoList.add(new VideoRank(sims[pi], 
						(String)documents.get(pIds[pi]).getName()));
			}
			if(videoList.isEmpty()) 
				continue;
			else {
				VideoRank[] videos = new VideoRank[videoList.size()];
				videoList.toArray(videos);
				Arrays.sort(videos);
				StringBuilder sb = new StringBuilder();
				sb.append(qIdString + ":1:");
				for(int vi=0; vi<videos.length-1; vi++) {
					sb.append(videos[vi].id + "|");
					int val = (int)videos[vi].sim;
					sb.append(val);
					sb.append(",");
				}
				sb.append(videos[videos.length-1].id + "|");
				int v = (int)videos[videos.length-1].sim;
				if (v < FREQ) continue;
				sb.append(v);
				
				qWriter.write(qIdString);
				qWriter.newLine();
				tWriter.write(sb.toString());
				tWriter.newLine();
			}				
		}
		qWriter.flush();
		qWriter.close();
		tWriter.flush();
		tWriter.close();
	}
	
	public static void generateLinkFolds(String sourceFile, 
								String trainFile,
								String testFile,
								int fold) throws IOException {		
		TIntIntHashMap samples = new TIntIntHashMap();
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		String line;
		ArrayList<String> lines = new ArrayList<String> ();
		while( (line = reader.readLine()) != null) {
			lines.add(line);
		}
		reader.close();
		
		int sampleSize = (int) (lines.size()/fold);
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
	
	public static void generateFolds(String sourceFile, 
			int fold) throws IOException {		
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		String line;
		ArrayList<String> lines = new ArrayList<String> ();
		while( (line = reader.readLine()) != null) {
			lines.add(line);
		}
		reader.close();
		Collections.shuffle(lines);
		
		int sampleSize = (int) (lines.size()/fold);
		for(int i=0; i<fold; i++) {
			String filename = sourceFile + "." + i;
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			for(int j=sampleSize*i; j<sampleSize*(i+1) && j<lines.size(); j++) {
				writer.write(lines.get(j));
				writer.newLine();
			}
			writer.flush();
			writer.close();
		}
	}
	
	
	public static void main(String[] args) throws IOException {
//		generate("dataset/cora/links.txt", "dataset/cora/links.train.txt", "dataset/cora/links.test.txt",5);
//		generateFolds("dataset/cora/links.txt", 5);
		GenerateLinkFolds glf = new GenerateLinkFolds();
		glf.generateDataFolds("dataset/vlc/vlc_train.en.f8.mallet", 
				"dataset/pairs.csv", "dataset/vlc/folds", 5);
	}
}
