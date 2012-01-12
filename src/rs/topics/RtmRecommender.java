package rs.topics;

import cc.mallet.types.*;
import rs.topics.*;
import rs.util.vlc.Task1Solution;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class RtmRecommender extends TopicRecommender {

	RelationalTopicModel rtm;
	
	public RtmRecommender(RelationalTopicModel m) {
		super(m.documents);
		this.rtm = m;
	}
	
	/**
	 * Given a query id, return a list of 30 videos from test videos.
	 * @param qId
	 * @return
	 */
	public String topicRecommend(String qId, Task1Solution solver) {
		int qdocId = solver.idHash.get(qId);
		
		int test_size = solver.documents.size() - solver.testIndexStart;
		double[] predSim = new double[test_size];
		Arrays.fill(predSim, rtm.eta[0]);
		
		for(int i=0; i<test_size; i++) {
			
			for(int t=0; t<rtm.numOfTopics; t++) {
				predSim[i] += rtm.zbar[solver.testIndexStart+i][t] * rtm.zbar[qdocId][t] * rtm.eta[t+1];
			}
			
		}
		String line = solver.sortRecommendList(qdocId, predSim);
		return line;
	}
	
	public void calculateProb() {
		phi = new double[rtm.numTerms][rtm.numOfTopics];
		theta = new double[rtm.documents.size()][rtm.numOfTopics];
		
		for(int i=0; i<rtm.numTerms; i++) 
			for (int j=0; j<rtm.numOfTopics; j++) {
				phi[i][j] = (double) (rtm.termTopicCounts[i][j] + rtm.beta) / 
								(double) (rtm.topicTokenCounts[j]+rtm.beta);
			}
		
		for(int i=0; i<rtm.documents.size(); i++) 
			for (int j=0; j<rtm.numOfTopics; j++) {
				FeatureSequence fs = (FeatureSequence)rtm.documents.get(i).getData();
				theta[i][j] = (double)(rtm.docTopicCounts[i][j] + rtm.alpha) /
									(double) (fs.getLength() + rtm.alpha);
			}
	}
	
	public void setRtm(RelationalTopicModel r) {
		this.rtm = r;
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String objectFile = "dataset/rtm.100.3000.dat";
		String solutionFile = "dataset/task1_solution.en.f8.100.3000.lm.txt";
		String queryFile = "dataset/task1_query.en.f8.txt";
		String targetFile = "dataset/task1_target.en.f8.txt";
		
		ObjectInputStream input = new ObjectInputStream(new FileInputStream(objectFile));
		RelationalTopicModel rtm = (RelationalTopicModel) input.readObject();
		RtmRecommender tester = new RtmRecommender(rtm);
		
		tester.recommendSolution(queryFile, solutionFile);
		try {
			System.out.println("RtmRecommender: " + Task1Solution.evaluateResult(targetFile, solutionFile));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
