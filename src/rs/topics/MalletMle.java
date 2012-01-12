/**
 * This model retrieves documents using language model based on MLE. 
 * More specifically, P_(mle)(t|M_d) = \frac{tf_{t,d}}/L_d
 * P
 */
package rs.topics;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import rs.util.vlc.Task1Solution;

import cc.mallet.types.*;

public class MalletMle extends MalletTfidf {
	public int[] collTf; 	// term frequency in the whole collection, indexed by term.
	public double[] collTw;	// term weight in the whole collection, indexed by term
	public double[][] termProb; // term weight in each doc, indexed by <document, term>
	public double[][] twidf;	// termProb * idf
	
	public int totalTokens;
	public double lambda;
	public double miu;
	
	public MalletMle(InstanceList ll) {
		super(ll);
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
		collTf = new int[numOfTerms];
		collTw = new double[numOfTerms];
		termProb = new double[numOfDocs][];
		twidf = new double[numOfDocs][];
		Arrays.fill(collTf, 0);
		Arrays.fill(collTw, 0);
		for(int i=0; i<numOfDocs; i++) {
			termProb[i] = new double[tf[i].length];
			twidf[i] = new double[tf[i].length];
		}
		
		totalTokens = 0;
		for(int i=0; i<numOfDocs; i++) {			
			FeatureSequence fs = (FeatureSequence)documents.get(i).getData();
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
		solver = new Task1Solution(documents) {
			public String recommend(String qId) {
				int qdocId = idHash.get(qId);
				
				int test_size = documents.size() - testIndexStart;
				double[] predSim = new double[test_size];
				Arrays.fill(predSim, 0);
				for(int i=0; i<test_size; i++) {
					predSim[i] = getMleSim2(qdocId, testIndexStart+i);
//					predSim[i] = getTwidfSim(qdocId, testIndexStart+i);
				}
				String line = sortRecommendList(qdocId, predSim);
				return line;
			}
		};
	}
	
	protected void calculateTermWeight() {
		for(int doc=0; doc < numOfDocs; doc++) {
			FeatureSequence fs = (FeatureSequence) documents.get(doc).getData();
			int docLen = fs.getLength();
			for(int t=0; t<termIndex[doc].length; t++) {
				int term = termIndex[doc][t];
				termProb[doc][t] = (double) tf[doc][t] / (double)docLen;
			}
		}
	}
	
	protected void calculateTwidf() {
		for(int doc=0; doc < numOfDocs; doc++) {
			for(int t=0; t<termIndex[doc].length; t++) {
				int term = termIndex[doc][t];
				twidf[doc][t] = termProb[doc][t] * idf[term];
			}
		}
	}
	
	private double getMleSim(int qdocId, int targetDocId) {
		double sim = 1;
		double[] qTermProb = termProb[qdocId];
		double[] tTermProb = termProb[targetDocId];
		
		int[] qTerm = termIndex[qdocId];
		int[] tTerm = termIndex[targetDocId];
		
		int[] qTf = tf[qdocId];
		
		int qi = 0;
		int ti = 0;
		
		for (qi=0; qi<qTerm.length; qi++) {
			int freq = qTf[qi];
			int term = qTerm[qi];
			double weight = 0;
			while (ti < tTerm.length && qTerm[qi] > tTerm[ti]) {
				ti++;
			}
			if (ti>=tTerm.length || qTerm[qi]<tTerm[ti]) { // not present in document language model, using smoothed weight
				weight = (1 - lambda) * collTw[term];
			} else if(qTerm[qi] == tTerm[ti]) {
				weight = lambda * tTermProb[ti] + (1-lambda)*collTw[term];
			}
//			sim *= weight;
			sim *= Math.pow(weight, freq);
//			sim *= Math.pow(weight, freq) * idf[term];			
		}
		return sim;
	}
	
	/**
	 * Get similarity between a query doc and a target doc.
	 * @param tokens
	 * @param targetDocId
	 * @return
	 */
//	public double getMleSim(int[] tokens, int targetDocId) {
//		double sim = 1;
//		double[] qTermProb = termProb[]
//		return 0;
//	}
	
	private double getMleSim2(int qdocId, int targetDocId) {
		lambda = (double) docLength[targetDocId] / (double)(docLength[targetDocId] + miu);
		return getMleSim(qdocId, targetDocId);
	}
	
	private double getTwidfSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		double[] qTwidf = twidf[qdocId];
		double[] tTwidf = twidf[targetDocId];
		
		int[] qTerm = termIndex[qdocId];
		int[] tTerm = termIndex[targetDocId];
		
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
		return sim/(normalizeVector(qTwidf) * normalizeVector(tTwidf));
	}
		
	public static void main(String[] args) throws IOException {
		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
		String queryFile = "dataset/task1_query.en.f8.txt";
		String targetFile = "dataset/task1_target.en.f8.txt";
		String solutionFile = "dataset/task1_solution.en.f8.tfidf.txt";
		InstanceList documents = InstanceList.load(new File(malletFile));
		double lambda=0.001;
		double miu = 100;
		MalletMle mt = new MalletMle(documents);
		
		while(miu < 10000){
//		while(lambda < 1) {
			mt.setLambda(lambda);
			mt.setMiu(miu);
			mt.retrieveTask1Solution(queryFile, solutionFile);
			try {
				double precision = Task1Solution.evaluateResult(targetFile, solutionFile);
				System.out.println("Miu: " + miu + ", Lambda: " + lambda + ", Mle precision: " + precision);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			miu *= 1.5;
			lambda *= 1.5;
		}
	}
}