package rs.util.vlc.solution;

/**
 * This class works as a super class for Task1Solution 
 * and Task1SolutionWithSeparateData.
 */

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.*;

import org.tunedit.core.ExternalProcess;
import org.tunedit.core.ResourceLoader;
import org.tunedit.core.ResourceName;
import org.tunedit.core.exception.TunedTesterException;

import rs.model.Model;
import rs.types.*;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public abstract class BasicTask1Solution {
	
<<<<<<< HEAD
	public final static int LIST_SIZE = 20;
//	public final static int RANK_SIZE = 5;
=======
	public final static int LIST_SIZE = Task1Evaluation.u_most;
	public final static int RANK_SIZE = 5;
>>>>>>> 95e6ed93f43c1ca8b6685cbba3a41c3b4be98076

	public InstanceList documents;
	public TObjectIntHashMap<String> idHash;
	public String[] testDocIds;
	
	Model model;
	
	public BasicTask1Solution() {
	}
	
	public BasicTask1Solution(Model model) {
		this.model = model;
		this.documents = model.getTrainingDocuments();
		initIdHash();
	}
	
	/**
	 * Init idHahs according to documents, here it is an instance list.
	 */
	protected void initIdHash() {
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
	
	protected abstract void initTestDocids();
	
	/**
	 * This method will calculates the top ranked videos' doc indices, so 
	 * that we can get a list of candidates, and rank them using different methods.
	 * @param predSimm
	 * @return
	 */
	public int[] topRankedVideoIndices(double[]predSim, int rank) {
		int[] indicesList = new int[rank];
		VideoRankIndices[] videos = new VideoRankIndices[predSim.length];
		for(int i=0; i<predSim.length; i++) {
			videos[i] = new VideoRankIndices(predSim[i], i);
		}
		Arrays.sort(videos);
		for(int i=0; i<rank; i++) {
			indicesList[i] = videos[i].index;
		}
		return indicesList;
	}
	
	/**
	 * Sort the filtered list of videos using new similarities.
	 * @param qdocId
	 * @param predSim
	 * @param idxList The indices of the filtered videos using a different measure. E.g.,
	 * 				  we first use LDA to create this list, then use tfidf to rank it. 
	 * @return
	 */
	public String sortFilteredRecommendList(int qdocId, double[] predSim, 
						int[] idxList,	String[] testDocIds) {
		String testVideoId = (String)documents.get(qdocId).getName();
		
		VideoRank[] videos = new VideoRank[idxList.length];
		for(int i=0; i<Math.min(idxList.length, predSim.length); i++) {
			int idx = idxList[i];
			videos[i] = new VideoRank(predSim[idx], testDocIds[idx]);
		}
		Arrays.sort(videos);
		StringBuilder line = new StringBuilder();
		
		line.append(testVideoId);
		line.append(":");
		for(int i=0; i<LIST_SIZE-1; i++) {
			line.append(videos[i].id);
			line.append(",");
		}
		line.append(videos[LIST_SIZE-1].id);
		return line.toString();
	}
	
	/**
	 * Same function like sortRecommendList, just use a different way. 
	 * @param qdocId
	 * @param predSim
	 * @return
	 */
	public String sortRecommendList2(int qdocId, double[] predSim, String[] testDocIds) {
		String testVideoId = (String)documents.get(qdocId).getName();
		int[] topDocs = topRankedVideoIndices(predSim, LIST_SIZE);
		StringBuilder line = new StringBuilder();
		line.append(testVideoId);
		line.append(":");
		for(int i=0; i<LIST_SIZE-1; i++) {
			line.append(testDocIds[topDocs[i]]);
			line.append(",");
		}
		line.append(testDocIds[topDocs[LIST_SIZE-1]]);
		return line.toString();
	}
	
	/**
	 * Recommendation list for one test video
	 * @param qdocId
	 * @param predSim
	 * @return
	 */
	public String sortRecommendList(int qdocId, double[] predSim, String[] testDocIds) {
		String testVideoId = (String)documents.get(qdocId).getName();
		VideoRank[] videos = new VideoRank[predSim.length];
		for(int i=0; i<predSim.length; i++) {
			videos[i] = new VideoRank(predSim[i], testDocIds[i]);
		}
		Arrays.sort(videos);
		StringBuilder line = new StringBuilder();
		
		line.append(testVideoId);
		line.append(":");
		for(int i=0; i<LIST_SIZE-1; i++) {
			line.append(videos[i].id);
			line.append(",");
		}
		line.append(videos[LIST_SIZE-1].id);
		return line.toString();
	}
	
	/**
	 * Sort the filtered list of videos using new similarities.
	 * @param qdocId
	 * @param predSim
	 * @param idxList The indices of the filtered videos using a different measure. E.g.,
	 * 				  we first use LDA to create this list, then use tfidf to rank it. 
	 * @return
	 */
	public String sortFilteredRecommendList(int qdocId, double[] predSim, int[] idxList) {
		return sortFilteredRecommendList(qdocId, predSim, idxList, testDocIds);
	}
	
	/**
	 * Same function like sortRecommendList, just use a different way. 
	 * @param qdocId
	 * @param predSim
	 * @return
	 */
	public String sortRecommendList2(int qdocId, double[] predSim) {
		return sortRecommendList2(qdocId, predSim, testDocIds);
	}
	
	/**
	 * Recommendation list for one test video
	 * @param qdocId
	 * @param predSim
	 * @return
	 */
	public String sortRecommendList(int qdocId, double[] predSim) {
		return sortRecommendList(qdocId, predSim, testDocIds);
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
		Task1Evaluation a = new Task1Evaluation(RANK_SIZE);		
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
		
<<<<<<< HEAD
		Double[] result = a.run(userLabelsName, targetLabelsName, loader);
//		Double[] result = a.runRKL(userLabelsName, targetLabelsName, loader);
=======
//		Double[] result = a.run(userLabelsName, targetLabelsName, loader);
		Double[] result = a.runRKL(userLabelsName, targetLabelsName, loader);
>>>>>>> 95e6ed93f43c1ca8b6685cbba3a41c3b4be98076
		return result[0];
	}
}
