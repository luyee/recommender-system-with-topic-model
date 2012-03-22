/**
 * This model retrieves documents using language model based on MLE. 
 * More specifically, P_(mle)(t|M_d) = \frac{tf_{t,d}}/L_d
 * P
 */
package rs.model.ir;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import rs.model.Model;
import rs.util.vlc.Util;
import rs.util.vlc.solution.Task1Solution;

import cc.mallet.types.*;

public class MalletMle extends Model {
	public MalletTfidf tfidf;
	public InstanceList documents;
	
	public int[] collTf; 	// term frequency in the whole collection, indexed by term.
	public double[] collTw;	// term weight in the whole collection, indexed by term
	public double[][] termProb; // term weight in each doc, indexed by <document, term>
	public double[][] twidf;	// termProb * idf
	
	public int totalTokens;
	public double lambda;
	public double miu;
	public int numOfTerms;
	public int numOfDocs;
	
	public MalletMle() {
		super();
	}
	
	public MalletMle(InstanceList ll) {
		tfidf = new MalletTfidf(ll);
		initMle();
	}
	
	public MalletMle(InstanceList ll, double lam) {
		this(ll);
		this.lambda = lam;
	}
	
	public void setLambda(double lam) {
		this.lambda = lam;
	}
	
	public void setMiu(double m) {
		this.miu = m;
	}
	
	public void initMle() {
		this.documents = tfidf.documents;

		this.numOfDocs = tfidf.numOfDocs;
		this.numOfTerms = tfidf.numOfTerms;
		
		collTf = new int[numOfTerms];
		collTw = new double[numOfTerms];
		termProb = new double[numOfDocs][];
		twidf = new double[numOfDocs][];
		Arrays.fill(collTf, 0);
		Arrays.fill(collTw, 0);
		for(int i=0; i<tfidf.numOfDocs; i++) {
			termProb[i] = new double[tfidf.tf[i].length];
			twidf[i] = new double[tfidf.tf[i].length];
		}
		
		totalTokens = 0;
		for(int i=0; i<numOfDocs; i++) {			
			FeatureSequence fs = (FeatureSequence)tfidf.documents.get(i).getData();
			totalTokens += fs.getLength();
			
			for(int t=0; t<fs.getLength(); t++) {
				int term = fs.getIndexAtPosition(t);
				collTf[term]++;
			}
		}
		System.out.println("total tokens: " + totalTokens);
		for(int i=0; i<numOfTerms; i++) {
			collTw[i] = (double)collTf[i] / (double)totalTokens;
		}
		calculateTermWeight();		
	}
	
	public void initSolver() {
		calculateTwidf();
	}
	
	protected void calculateTermWeight() {
		for(int doc=0; doc < numOfDocs; doc++) {
			FeatureSequence fs = (FeatureSequence) documents.get(doc).getData();
			int docLen = fs.getLength();
			for(int t=0; t<tfidf.termIndex[doc].length; t++) {
				termProb[doc][t] = (double) tfidf.tf[doc][t] / (double)docLen;
			}
		}
	}
	
	/**
	 * Incorporate global weight of inverse-document-frequency
	 */
	protected void calculateTwidf() {
		for(int doc=0; doc < numOfDocs; doc++) {
			for(int t=0; t<tfidf.termIndex[doc].length; t++) {
				int term = tfidf.termIndex[doc][t];
				twidf[doc][t] = termProb[doc][t] * tfidf.idf[term];
			}
		}
	}
	
	private double getMleSim(int qdocId, int targetDocId) {
		double sim = 1;
		double[] qTermProb = termProb[qdocId];
		double[] tTermProb = termProb[targetDocId];
		
		int[] qTerm = tfidf.termIndex[qdocId];
		int[] tTerm = tfidf.termIndex[targetDocId];
		
		int[] qTf = tfidf.tf[qdocId];
		int[] tTf = tfidf.tf[targetDocId];
		
		int qi = 0;
		int ti = 0;
		
		for (ti=0; ti<tTerm.length; ti++) {
			int freq = tTf[ti];
			int term = tTerm[ti];
			double weight = 0;
			weight = (1-lambda) * collTw[term];
			
			int pos = Arrays.binarySearch(qTerm, term);
			if (pos >= 0) {
				weight += lambda * qTermProb[pos];
			}
			sim *= Math.pow(weight, freq);
			
			/*********************/
			sim /= Math.pow( collTw[term], freq);
			/*********************/
		}
		
//		for (qi=0; qi<qTerm.length; qi++) {
//			int freq = qTf[qi];
//			int term = qTerm[qi];
//			double weight = 0;
//			/*********/
//			weight = (1-lambda) * collTw[term];
//			int pos = Arrays.binarySearch(tTerm, term);
//			if (pos >= 0) {
//				weight += lambda * tTermProb[pos];
//			}
//			/********/
//			while (ti < tTerm.length && qTerm[qi] > tTerm[ti]) {
//				ti++;
//			}
//			if (ti>=tTerm.length || qTerm[qi]<tTerm[ti]) { // not present in document language model, using smoothed weight
//				weight = lambda * collTw[term];
//			} else if(qTerm[qi] == tTerm[ti]) {
//				weight = (1 - lambda) * tTermProb[ti] + lambda * collTw[term];
//			}
//			sim *= Math.pow(weight, freq);
//		}
		return sim;
	}
	
	private double getMleKLDivergenceSim(int qdocId, int targetDocId) {
		double sim = 0;
		double[] qTermProb = termProb[qdocId];
		double[] tTermProb = termProb[targetDocId];
		
		int[] qTerm = tfidf.termIndex[qdocId];
		int[] tTerm = tfidf.termIndex[targetDocId];
		
		int[] qTf = tfidf.tf[qdocId];
		int[] tTf = tfidf.tf[targetDocId];
		
		int qi = 0;
		int ti = 0;
		
		for (qi=0; qi<qTerm.length; qi++) {
			int term = qTerm[qi];
			int pos = Arrays.binarySearch(tTerm, term);
//			double qWeight = lambda * collTw[term] + (1-lambda) * qTermProb[qi];
//			double tWeight = lambda * collTw[term];
//			if (pos >= 0) {
//				tWeight += (1-lambda) * tTermProb[pos];
//				sim += qWeight * Math.log(qWeight / tWeight);
//			}
			if (pos >= 0) {
				sim += qTermProb[qi] * Math.log(qTermProb[qi] / tTermProb[pos]);
			}
			
		}
		
//		for (ti=0; ti<tTerm.length; ti++) {
//			int term = tTerm[ti];
//			double tWeight = lambda * collTw[term] + (1-lambda) * tTermProb[ti];
//			int pos = Arrays.binarySearch(qTerm, term);
//			if (pos < 0) {
//				double qWeight = lambda * collTw[term];
//				sim += qWeight * Math.log(qWeight / tWeight);
//			}
//		}
		return sim;
	}
	
	private double getMleSim2(int qdocId, int targetDocId) {
		lambda = (double) tfidf.docLength[targetDocId] / (double)(tfidf.docLength[targetDocId] + miu);
		return getMleSim(qdocId, targetDocId);
	}
	
	public double getTermProbVSM(int qdocId, int targetDocId) {
		double sim = 0;
		double[] qTermProb = termProb[qdocId];
		double[] tTermProb = termProb[targetDocId];
		
		int[] qTerm = tfidf.termIndex[qdocId];
		int[] tTerm = tfidf.termIndex[targetDocId];
		
		int qi = 0;
		int ti = 0;
		
		for (ti=0; ti<tTerm.length; ti++) {
			int term = tTerm[ti];
			double weight = 0;
			
			int pos = Arrays.binarySearch(qTerm, term);
			if (pos >= 0) {
				sim += tTermProb[ti] * qTermProb[pos];
			}
		}
		sim /= Util.normalizeVector(qTermProb) * Util.normalizeVector(tTermProb);
		return sim;
	}
	
	public double getTwidfSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		double[] qTwidf = twidf[qdocId];
		double[] tTwidf = twidf[targetDocId];
		
		int[] qTerm = tfidf.termIndex[qdocId];
		int[] tTerm = tfidf.termIndex[targetDocId];
		
		int qi = 0;
		int ti = 0;
		
		double sim = 0;
		
		while(qi<qTerm.length && ti<tTerm.length) {
			if(qTerm[qi] == tTerm[ti]) {
				sim += qTwidf[qi] * tTwidf[ti];
				qi++;
				ti++;
			} else {
				if (qTerm[qi] < tTerm[ti]) 
					qi++;
				else 
					ti++;
			}
		}
		return sim/(Util.normalizeVector(qTwidf) * Util.normalizeVector(tTwidf));
	}
	

	@Override
	public double getSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		return getMleSim(qdocId, targetDocId);
	}

	@Override
	public InstanceList getTrainingDocuments() {
		// TODO Auto-generated method stub
		return this.documents;
	}
	
}