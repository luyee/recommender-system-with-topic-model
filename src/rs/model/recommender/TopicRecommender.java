package rs.model.recommender;

import cc.mallet.types.*;
import rs.model.Model;

public abstract class TopicRecommender extends Model {
	InstanceList documents;
	public double[][] phi;				//term, topic probability, indexed by term, topic
	public double[][] theta;			//document, topic probability, indexed by doc, topic

	public TopicRecommender(InstanceList documents) {
		this.documents = documents;
	}
	
	public InstanceList getTrainingDocuments() { 
		return this.documents;
	}
	
	public abstract void calculateProb();

	public double getSim(int qdocId, int targetDocId) {
		return queryLikelihoodModel(qdocId, targetDocId);
	}
	
	/**
	 * Get predSimilarity for query doc, using language model
	 * @param qdocId
	 * @param testStartId
	 * @param testSize
	 * @return
	 */
	public double queryLikelihoodModel(int qdocId, int targetDocId) {
		double predSim = 1;
		FeatureSequence fs = (FeatureSequence) getTrainingDocuments().get(qdocId).getData();
		for(int t=0; t < fs.getLength(); t++) {
			double termLikelihood = 0;
			int term = fs.getIndexAtPosition(t);
			for(int topic = 0; topic < theta[0].length; topic++) {
				if(phi[term][topic] == 0) System.out.println(term + ", " + topic + " is 0");
				if(theta[targetDocId][topic] == 0) System.out.println(targetDocId + ", " + topic + " is 0");
				
				termLikelihood += phi[term][topic]*theta[targetDocId][topic];
			}
			predSim *= termLikelihood;
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
	public double queryLikelihoodModel2 (int qdocId, int targetDocId) {
		double predSim = 1;
		FeatureSequence fs = (FeatureSequence) getTrainingDocuments().get(targetDocId).getData();
		for(int t=0; t<fs.getLength(); t++) {
			double termLikelihood = 0;
			int term = fs.getIndexAtPosition(t);
			for(int topic = 0; topic < theta[0].length; topic++) {
				if(phi[term][topic] == 0) System.out.println(term + ", " + topic + " is 0");
				if(theta[targetDocId][topic] == 0) System.out.println(targetDocId + ", " + topic + " is 0");
				termLikelihood += phi[term][topic]*theta[qdocId][topic];
			}
			predSim *= termLikelihood;
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
	public double queryTopicVSM (int qdocId, int targetDocId) {
		double predSim = 0;
		
		double[] v1 = theta[qdocId];
		double[] v2 = theta[targetDocId];
		predSim = rs.util.vlc.Util.cosineProduct(v1, v2);
		
		return predSim;
	}

	
}
