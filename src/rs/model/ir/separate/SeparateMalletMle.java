package rs.model.ir.separate;

import java.util.Arrays;

import gnu.trove.map.hash.TObjectIntHashMap;
import rs.model.SeparateModel;
import rs.model.ir.MalletMle;
import rs.util.vlc.solution.Task1Solution;
import cc.mallet.types.*;

public class SeparateMalletMle extends SeparateModel {
	MalletMle training;
	MalletMle test;
	
	public SeparateMalletMle(InstanceList trainDocs, InstanceList testDocs) {
		training = new MalletMle(trainDocs);
		test = new MalletMle(testDocs);
	}
	
	public void setLambda(double lambda) {
		training.setLambda(lambda);
	}
	
	private double getSeparateMleSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		double lambda = training.lambda;
		double sim = 1;
		double[] qTermProb = training.termProb[qdocId];
		double[] tTermProb = test.termProb[targetDocId];
		
		int[] qTerm = training.tfidf.termIndex[qdocId];
		int[] tTerm = test.tfidf.termIndex[targetDocId];
		
		int[] qTf = training.tfidf.tf[qdocId];
		int[] tTf = test.tfidf.tf[targetDocId];
		
		int qi = 0;
		int ti = 0;
		
		for (ti=0; ti<tTerm.length; ti++) {
			int freq = tTf[ti];
			int term = tTerm[ti];
			double weight = 0;
			weight = (1-lambda) * training.collTw[term];
			
			int pos = Arrays.binarySearch(qTerm, term);
			if (pos >= 0) {
				weight += lambda * qTermProb[pos];
			}
			sim *= Math.pow(weight, freq);
			
			/*********************/
			sim /= Math.pow( training.collTw[term], freq);
			/*********************/
		}
		return sim;
	}


	@Override
	public InstanceList getTestDocuments() {
		// TODO Auto-generated method stub
		return test.getTrainingDocuments();
	}

	@Override
	public double getSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		return getSeparateMleSim(qdocId, targetDocId);
	}

	@Override
	public InstanceList getTrainingDocuments() {
		// TODO Auto-generated method stub
		return training.getTrainingDocuments();
	}
}
