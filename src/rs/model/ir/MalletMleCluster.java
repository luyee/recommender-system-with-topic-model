/**
 * Another way of smoothing language models, based on clustering. 
 * We use pseudo counts for each document instead of the original documents, 
 * constructed as: c(w, d') = alpha * c(w, d) + (1-alpha) * · (sim(d,b) * c(w,b)
 * Ultimately it will be smoothed like: 
 * P(w|D) = lambda * P_ML (w|D) + (1-lambda) * [ beta P_ML (w|cluster) + (1-beta) P_ML (w|coll)]  
 */
package rs.model.ir;

import java.io.IOException;
import java.util.Arrays;

import rs.types.PairData;
import rs.types.PairedInfo;
import rs.util.vlc.Util;
import rs.util.vlc.solution.Task1Solution;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.util.ArrayUtils;

public class MalletMleCluster extends MalletMle{
	PairData pairs;
	double alpha; 		// parameter to interpolate cluster based prob with collection prob.
	double beta;
	
	double[][] pseudoTf;  	// term frequency of pseudo documents
	double[][] pseudoTermWeight;
	int[][] pseudoTermIndex;// terms of pseudo documents
	
	public MalletMleCluster(InstanceList ll) {
		super(ll);
		// TODO Auto-generated constructor stub
	}

	public MalletMleCluster(String trainMalletFile, String linkFile, double alpha) {
		super();
		this.alpha = alpha;
		initPairs(trainMalletFile, linkFile);
		this.documents = pairs.documents;
		tfidf = new MalletTfidf(documents);
		initMle();
		initMleCluster();
	}
	
	public void setSmoothParameters(double alpha, double beta, double lambda) {
		this.alpha = alpha;
		this.beta = beta;
		this.lambda = lambda;
	}
	
	private void initMleCluster() {
		// TODO Auto-generated method stub
		pseudoTf = new double[documents.size()][];
		pseudoTermIndex = new int[documents.size()][];
		pseudoTermWeight = new double[documents.size()][];
		
		for(int docIdx=0; docIdx<documents.size(); docIdx++) {
			countOnePseudoDoc(docIdx);
		}
	}
	
	/**
	 * Calculate the count in pseudo documents.
	 * @param docIdx
	 * @param pTf
	 * @param pTw
	 * @param pTIdx
	 */
	private void countOnePseudoDoc(int docIdx) {
		FeatureSequence fs = (FeatureSequence)documents.get(docIdx).getData();
		int[] tokens = fs.getFeatures();
		double[] tmpCounts = new double[numOfTerms];
		Arrays.fill(tmpCounts, 0);
		for(int i=0; i<tokens.length; i++) {
			int term = tokens[i];
			tmpCounts[term] += alpha * 1;
		}
		
		PairedInfo pairedDocs = pairs.links.get(docIdx);
		if(!pairedDocs.isEmpty()) {
//			System.out.println(docIdx + ":" + Arrays.toString(tokens));
			int[] pIds = pairedDocs.getPairedIdsArray();
//			System.out.println(docIdx + ": " + pairedDocs.getLength() + ":" + Arrays.toString(pIds));
			double[] sims = pairedDocs.getPairedSimArray();
			sims = Util.reWeightVector(sims);
			for(int p=0; p<pIds.length; p++) {
				fs = (FeatureSequence)documents.get(p).getData();
				int[] pTokens = fs.getFeatures();
				for(int i=0; i<pTokens.length; i++) {
					int term = pTokens[i];
					tmpCounts[term] += (1-alpha) * sims[p];
				}
				tokens = ArrayUtils.append(tokens, fs.getFeatures());
			}
//			System.out.println(docIdx + ":" + Arrays.toString(tokens));
		}
		
		int termNum = MalletTfidf.distinctNumbers(tokens);
		pseudoTermIndex[docIdx]= new int[termNum];
		pseudoTf[docIdx] = new double[termNum];
		int[] tmpTf = new int[termNum];
		MalletTfidf.countTf(tokens, pseudoTermIndex[docIdx], tmpTf);  // count the tf frequency
		for(int i=0; i<pseudoTermIndex[docIdx].length; i++) {
			int term = pseudoTermIndex[docIdx][i];
			pseudoTf[docIdx][i] = tmpCounts[term];
		}
		pseudoTermWeight[docIdx] = Util.reWeightVector(pseudoTf[docIdx]);
	}

	private void initPairs(String trainMalletFile, String linkFile) {
		pairs = new PairData();
		try {
			pairs.initFromFile(trainMalletFile, linkFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Using target language model.
	 * @param qdocId
	 * @param targetDocId
	 * @return
	 */
	protected double getClusterMleSim2(int qdocId, int targetDocId) {
		System.out.println(qdocId);

		double sim = 1;
		double[] qTermProb = termProb[qdocId];
		double[] tTermProb = termProb[targetDocId];
		double[] qPseudoProb = pseudoTermWeight[qdocId];
		
		int[] qTerm = tfidf.termIndex[qdocId];
		int[] tTerm = tfidf.termIndex[targetDocId];
		int[] qPseudoTerm = pseudoTermIndex[qdocId];
		
		int[] qTf = tfidf.tf[qdocId];
		int[] tTf = tfidf.tf[targetDocId];
		double[] qPseudoTf = pseudoTf[qdocId];
		
		int qi = 0;
		int pseudoQi = 0;
		int ti = 0;
		
		for (qi=0; qi<qPseudoTerm.length; qi++) {
			double pseuduFreq = qPseudoTf[qi];
			int term = qPseudoTerm[qi];
			double weight = 0; 
			int pos = Arrays.binarySearch(tTerm, term);
			if (pos >= 0) {
				weight += lambda * tTermProb[pos];
			}
			weight += (1-lambda)  * collTw[term];
			sim *= Math.pow(weight, pseuduFreq);
			
			/*****************/
//			sim /= Math.pow(collTw[term], freq);
			/*****************/
		}
		return sim;
	}
	
	public double getSim(int qdocId, int targetDocId) {
		return getClusterMleSim(qdocId, targetDocId);
	}
	
	private double getClusterMleSim(int qdocId, int targetDocId) {
//		System.out.println(qdocId);
		double sim = 1;
		double[] qTermProb = termProb[qdocId];
		double[] tTermProb = termProb[targetDocId];
		double[] qPseudoProb = pseudoTermWeight[qdocId];
		
		int[] qTerm = tfidf.termIndex[qdocId];
		int[] tTerm = tfidf.termIndex[targetDocId];
		int[] qPseudoTerm = pseudoTermIndex[qdocId];
		
		int[] qTf = tfidf.tf[qdocId];
		int[] tTf = tfidf.tf[targetDocId];
		
		int qi = 0;
		int pseudoQi = 0;
		int ti = 0;
		
		for (ti=0; ti<tTerm.length; ti++) {
			int freq = tTf[ti];
			int term = tTerm[ti];
			double weight = 0; 
			int pos = Arrays.binarySearch(qTerm, term);
			if (pos >= 0) {
				weight += lambda * qTermProb[pos];
			}
			
			pos = Arrays.binarySearch(qPseudoTerm, term);
			if (pos >= 0) {
				weight += (1-lambda) * beta * qPseudoProb[pos];
			}
				
			weight += (1-lambda) * (1-beta) * collTw[term];
			if(weight == 0) continue;
			sim *= Math.pow(weight, freq);
			
			/*****************/
			sim /= Math.pow(collTw[term], freq);
			/*****************/
		}
		return sim;
	}

	public static void main(String[] args) {
//		String malletFile = "dataset/vlc/vlc_lectures.all.en.f8.mallet";
		String solutionFile = "dataset/vlc/task1_solution.en.clusterMle.txt";
//		String queryFile = "dataset/vlc/task1_query.en.f8.n5.txt";
//		String targetFile = "dataset/vlc/task1_target.en.f8.n5.txt";
//		String linkFile = "dataset/vlc/sim_0p_100n.csv";
		
		String malletFile = "dataset/vlc/folds/all.0.4189.mallet";
		String queryFile = "dataset/vlc/folds/query.0.csv";
		String targetFile = "dataset/vlc/folds/target.0.csv";
		String linkFile = "dataset/vlc/folds/trainingPairs.0.csv";
		
		double alpha = 0.01, beta = 0.01, lambda = 0.01;
		
		MalletMleCluster clusterMle = new MalletMleCluster(malletFile, linkFile, alpha);
		Task1Solution solver = new Task1Solution(clusterMle);
//		for(alpha = 0.1; alpha<1; alpha += 0.2) {
//			for(beta = 0.1; beta<1; beta += 0.2) {
				for (lambda = 0.1; lambda<1; lambda += 0.05) {
			clusterMle.setSmoothParameters(alpha, beta, lambda);
//			clusterMle.retrieveTask1Solution(queryFile, solutionFile);
			try {
				solver.retrieveTask1Solution(queryFile, solutionFile);
				double precision = Task1Solution.evaluateResult(targetFile, solutionFile);
				System.out.println("alpha: " + alpha + "beta: " + beta + 
						", Lambda: " + lambda + ", Mle precision: " + precision);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				}
//			}
//		}
	}
}
