/**
 * This class is similar to MalletTfidf and MalletMle. Difference is instead of using 
 * tfidf, we change tf into term probability calculated from topic model, then 
 * calculate term weight with termProb * idf.
 */
package rs.text.recommender;

import java.io.IOException;
import java.util.Arrays;


import cc.mallet.types.*;

public abstract class TopicIdfRecommender extends TopicRecommender {
	public MalletTfidf tfidfRecommender;
	
	public TopicIdfRecommender(InstanceList documents) {
		super(documents);
		this.tfidfRecommender = new MalletTfidf(documents);
		tfidfRecommender.initSolver();
		this.solver = tfidfRecommender.solver;
	}
	
	public void calculateTopicIdf() {
		if(theta == null) calculateProb();
		int numOfDocs = tfidfRecommender.numOfDocs;
		int[][] termIndex = tfidfRecommender.termIndex;
		double[][] topicIdf = tfidfRecommender.tfidf.clone();
		double[] idf = tfidfRecommender.idf;
		
		for(int doc=0; doc < numOfDocs; doc++) {
			FeatureSequence fs = (FeatureSequence) solver.documents.get(doc).getData();
			for(int t=0; t<termIndex[doc].length; t++) {
				int term = termIndex[doc][t];
				double termLikelihood = 0;
				for(int topic = 0; topic < theta[0].length; topic++) {
					if(phi[term][topic] == 0) System.out.println(term + ", " + topic + " is 0");
					if(theta[doc][topic] == 0) System.out.println(doc + ", " + topic + " is 0");
					termLikelihood += phi[term][topic]*theta[doc][topic];
				}
				topicIdf[doc][t] = termLikelihood * idf[term];
			}
		}
	}
	
	public void retrieveTask1Solution(String queryFile, String solutionFile) {
		calculateTopicIdf();
		tfidfRecommender.retrieveTask1Solution(queryFile, solutionFile);
	}
}
