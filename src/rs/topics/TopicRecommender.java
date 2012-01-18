package rs.topics;

import java.io.IOException;
import java.util.Arrays;

import cc.mallet.types.*;
import rs.util.vlc.Task1Solution;

public abstract class TopicRecommender {
	public double[][] phi;				//term, topic probability, indexed by term, topic
	public double[][] theta;			//document, topic probability, indexed by doc, topic

	public Task1Solution solver;

	public abstract void calculateProb();
	
	public void setSolver(Task1Solution solver) {
		this.solver = solver;
	}

	public TopicRecommender(InstanceList documents) {
		initSolver(documents);
	}

	protected void initSolver(InstanceList documents) {
		solver = new Task1Solution(documents) {
			/* P(Q|D) = \prod{P(q|D} */
			public String recommend(String  qId) {
				int qdocId = idHash.get(qId);
				
				int test_size = documents.size() - testIndexStart;
				double[] predSim = queryLikelihoodModel(qdocId, testIndexStart, test_size);
//				double[] predSim = queryLikelihoodModel2(qdocId, testIndexStart, test_size);
						
				String line = sortRecommendList(qdocId, predSim);
				return line;
			}
		};
	}
	
	/**
	 * Get predSimilarity for query doc, using language model
	 * @param qdocId
	 * @param testStartId
	 * @param testSize
	 * @return
	 */
	public double[] queryLikelihoodModel(int qdocId, int testIndexStart, int testSize) {
		double[] predSim = new double[testSize];
		Arrays.fill(predSim, 1);
		FeatureSequence fs = (FeatureSequence) solver.documents.get(qdocId).getData();
		for(int i=0; i<testSize; i++) {			
			int testId = testIndexStart + i;
			for(int t=0; t < fs.getLength(); t++) {
				double termLikelihood = 0;
				int term = fs.getIndexAtPosition(t);
				for(int topic = 0; topic < theta[0].length; topic++) {
					if(phi[term][topic] == 0) System.out.println(term + ", " + topic + " is 0");
					if(theta[testId][topic] == 0) System.out.println(testId + ", " + topic + " is 0");
					
					termLikelihood += phi[term][topic]*theta[testId][topic];
				}
				predSim[i] *= termLikelihood;
			}
		}
		return predSim;
	}
	
	/**
	 * This is different from above method in the aspect that sim=p(testDoc | queryDoc) instead  of the opposite. 
	 * @param qdocId
	 * @param fs	Feature sequence of query document.
	 * @param testStartId
	 * @param testSize
	 * @return
	 */
	public double[] queryLikelihoodModel2 (int qdocId, int testIndexStart, int testSize) {
		double[] predSim = new double[testSize];
		Arrays.fill(predSim, 1);
		
		for(int i=0; i<testSize; i++) {			
			int testId = testIndexStart + i;
			FeatureSequence fs = (FeatureSequence) solver.documents.get(testId).getData();
			for(int t=0; t<fs.getLength(); t++) {
				double termLikelihood = 0;
				int term = fs.getIndexAtPosition(t);
				for(int topic = 0; topic < theta[0].length; topic++) {
					if(phi[term][topic] == 0) System.out.println(term + ", " + topic + " is 0");
					if(theta[testId][topic] == 0) System.out.println(testId + ", " + topic + " is 0");
					termLikelihood += phi[term][topic]*theta[qdocId][topic];
				}
				predSim[i] *= termLikelihood;
			}
		}
		return predSim;
	}
	
	/**
	 * Get predicted similarity for query doc, using topic vector space model.
	 * It calculates similarity between two topic distribution vectors.
	 * @param qdocId
	 * @param testStartId
	 * @param testSize
	 * @return
	 */
	public double[] queryTopicVSM (int qdocId, int testIndexStart, int testSize) {
		double[] predSim = new double[testSize];
		Arrays.fill(predSim, 0);
		
		for(int i=0; i<testSize; i++) {
			double[] v1 = theta[qdocId];
			double[] v2 = theta[testIndexStart + i];
			predSim[i] = rs.util.vlc.Util.cosineProduct(v1, v2);
		}
		
		return predSim;
	}
	
	public void recommendSolution(String queryFile, String solutionFile) throws IOException {
		solver.retrieveTask1Solution(queryFile, solutionFile);
	}
	
	public void retrieveTask1Solution(String queryFile, String solutionFile) {
		if(solver == null) return;
		try {
			solver.retrieveTask1Solution(queryFile, solutionFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
