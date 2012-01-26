package rs.util.vlc;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.*;

import org.tunedit.core.ExternalProcess;
import org.tunedit.core.ResourceLoader;
import org.tunedit.core.ResourceName;
import org.tunedit.core.exception.TunedTesterException;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public abstract class Task1Solution {

	public final static int LIST_SIZE = 30;
	public InstanceList documents;
	public static final int testIndexStart = 5236;

	public TObjectIntHashMap<String> idHash;
	
	public Task1Solution() {
	}
	
	public Task1Solution(InstanceList doc) {
		setDocuments(doc);
	}
	
	public void setDocuments(InstanceList il) {
		this.documents = il;
		initIdHash();
	}
	
	/**
	 * Init idHahs according to documents, here it is an instance list.
	 */
	private void initIdHash() {
		if (documents == null) {
			System.err.println("Document instance should be initialized first.");
			return;
		}
		if(idHash == null) {
			idHash = new TObjectIntHashMap<String>();
		}
		for(int i=0; i<documents.size(); i++) {
			Instance doc = documents.get(i);
			String vId = (String) doc.getName();
			idHash.put(vId, i);
		}
	}
	
	/**
	 * Recommendation list for one test video
	 * @param qdocId
	 * @param predSim
	 * @return
	 */
	public String sortRecommendList(int qdocId, double[] predSim) {
		String testVideoId = (String)documents.get(qdocId).getName();
		class VideoRank implements Comparable {
			double sim;
			String id;
			public VideoRank (double sim, String id) { this.sim = sim; this.id = id; }
			/**
			 * Sort in reverse order
			 * @param o2
			 * @return
			 */
			public final int compareTo (Object o2) {
				if (sim > ((VideoRank)o2).sim)
					return -1;
				else if (sim == ((VideoRank)o2).sim)
					return 0;
				else return 1;
			}
		}
		VideoRank[] videos = new VideoRank[predSim.length];
		for(int i=0; i<Math.min(testIndexStart, predSim.length); i++) {
			videos[i] = new VideoRank(predSim[i], (String)documents.get(this.testIndexStart+i).getName());
		}
		Arrays.sort(videos);
		StringBuilder line = new StringBuilder();
		
//		for(int i=0; i<LIST_SIZE; i++) {
//			line.append(testVideoId);
//			line.append(" ");
//			line.append(videos[i].id);
//			line.append(" ");
//			line.append(videos[i].sim);
//			line.append("\n");
//		}
//		return line.toString();
		
		line.append(testVideoId);
		line.append(":");
		for(int i=0; i<LIST_SIZE-1; i++) {
			line.append(videos[i].id);
			line.append(",");
		}
		line.append(videos[LIST_SIZE-1].id);
		return line.toString();
	}
	
	public abstract String recommend(String qid);

	
	public ArrayList<String> query(String queryFile) throws IOException {
		ArrayList<String>lines = new ArrayList<String>();
		BufferedReader reader =  new BufferedReader(new FileReader(queryFile));
		String qid;
		while( (qid = reader.readLine()) != null) {
			qid = qid.trim();
			lines.add(recommend(qid));
		}
		
		return lines;
	}
	
	public void retrieveTask1Solution(String queryFile, String solutionFile) 
			throws IOException {
		ArrayList<String> solution = query(queryFile);
		BufferedWriter writer = new BufferedWriter(new FileWriter(solutionFile));
		for(int i=0; i<solution.size(); i++) {
			writer.write(solution.get(i));
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}
	
	public static double evaluateResult(String targetFile, String solutionFile) throws Exception {
		Task1Evaluation a = new Task1Evaluation(LIST_SIZE);		
		ResourceName userLabelsName = new ResourceName(solutionFile);
		
		//put here the path to the file "task1_target_test_leaderboard.txt"
		ResourceName targetLabelsName = new ResourceName(targetFile);
		
		ResourceLoader loader = new ResourceLoader() {
			
			@Override
			public ExternalProcess runProcess(ResourceName fileResource,
					List<String> args) throws TunedTesterException {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public InputStream open(ResourceName fileResource)
					throws TunedTesterException {
				try {
					return new FileInputStream(fileResource.toString());
				} catch (FileNotFoundException e) {
					throw new TunedTesterException(e);
				}
			}
		};
		
		Double[] result = a.run(userLabelsName, targetLabelsName, loader);
		return result[0];
	}
}
