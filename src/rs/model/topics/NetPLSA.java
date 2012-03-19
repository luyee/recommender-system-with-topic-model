package rs.model.topics;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.*;

import rs.model.ir.MalletTfidf;
import rs.types.PairedInfo;
import rs.util.vlc.solution.Task1Solution;

import cc.mallet.types.*;
import cc.mallet.util.Randoms;

public class NetPLSA {
	public final static String BY = "x"; 
	public final static double EPS = 0.000000001;
	public final static int WFACTOR = 100;

	public int[][] termIndex;		// indexed by <document, term>, this indicates which terms appear in each doc
	public int[][] tf;				// indexed by <document, term>, indicating frequency of each term
	public int[] docLength;
	public double lambda;
	public double gamma;
	
	public double[][][] z_w_d;				// indexed by <document, word, topic>, indicating the topic for each token in each doc
	public double[][] p_theta_d;			// indexed by <document, topic>
	public double[][] p_w_theta;			// indexed by <topic, term>
	public TObjectIntHashMap<String> idHash;
	public TObjectIntHashMap<String> pairIdHash; 	// v1xv2 -> int

	public ArrayList<PairedInfo> links;
	public int numOfIterations;
	public int numOfTopics;	
	public int numOfLinks;
	public int numOfTerms;
	
	public InstanceList documents;
	public Task1Solution solver;
	
	/**
	 * Test documents
	 */
	public InstanceList testDocuments;
	public double[][] test_p_theta_d;
	
	public int[][] testTermIndex;
	public int[][] testTf;
	public int[] testDocLength;
	
	public NetPLSA(int numTopics, double lam, double gamma, String malletFile, String linkFile) {
		this.numOfTopics = numTopics;
		this.lambda = lam;
		this.gamma = gamma;
		
		initTf(malletFile);
		initLinks(numTopics, malletFile, linkFile);
		
	}

	private void initLinks(int numTopics, String malletFile, String linkFile) {
		RelationalTopicModel rtm = new RelationalTopicModel(numTopics);
		try {
			rtm.initFromFile(malletFile, linkFile);
			this.idHash = rtm.idHash;
			this.pairIdHash = rtm.pairIdHash;
			this.links = rtm.links;
			this.numOfLinks = rtm.numOfLinks;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initTf(String malletFile) {
		this.documents = InstanceList.load(new File(malletFile));;
		MalletTfidf tfidf = new MalletTfidf(documents);
		this.numOfTerms = tfidf.numOfTerms;
		this.tf = tfidf.tf;
		this.termIndex = tfidf.termIndex;
		this.docLength = tfidf.docLength;
	}
	
	/**
	 * Return the sim value of a specified pair. 
	 * 
	 * @param v1
	 * @param v2
	 * @return 0 means this pair does not exist.
	 */
	public double getPairedSim(int v1, int v2) {
		if (!links.get(v1).isEmpty())
			return links.get(v1).getSim(v2);
		return 0;
	}

	private void initArraySpace() {
		Randoms r = new Randoms();
		z_w_d = new double[documents.size()][][];
		for(int docIdx = 0; docIdx < documents.size(); docIdx++) {
			z_w_d[docIdx] = new double[termIndex[docIdx].length][numOfTopics];
//			for (int tokenIdx = 0; tokenIdx < termIndex[docIdx].length; tokenIdx++) {
//				z_w_d[docIdx][tokenIdx] = new double[numOfTopics];
//			}
		}
		
		p_w_theta = new double[numOfTopics][numOfTerms];
		p_theta_d = new double[documents.size()][numOfTopics];
		for (int i=0; i<numOfTopics; i++) {
			randomInitArray(r, p_w_theta[i]);
		}
		for (int i=0; i<documents.size(); i++) {
			randomInitArray(r, p_theta_d[i]);
		}
	}

	/**
	 * Randomly assign values between 0-1 to an array and normalize it.
	 * @param r
	 * @param out
	 */
	public void randomInitArray(Randoms r, double[] out) {
		for(int i=0; i<out.length; i++) {
			out[i] = r.nextUniform();
		}
		normalizeArray(out);
	}
	
	public void normalizeArray(double[] a) {
		double s = arraySum(a);
		if (s!=0 && a.length != 1) 
			arrayDivide(a, s);
	}
	
	public void arrayDivide(double[] a, double s) {
		for (int i=0; i<a.length; i++)
			a[i] = a[i]/s;
	}
	
	public double arraySum(double[] a) {
		double result = 0;
		for (int i=0; i<a.length; i++) 
			result += a[i];
		return result;
	}
	
	/**
	 * Add a constant to every element in an array
	 * @param a
	 * @param eps
	 */
	public void addConstant(double[] a, double eps) {
		for (int i=0; i<a.length; i++) 
			a[i] += eps;
	}
	
	/**
	 * Calculate weight regularization from links.
	 */
	private double calculateLinkRegularization() {
		double result = 0;
		TObjectIntIterator<String> iterator = pairIdHash.iterator();
		while(iterator.hasNext()) {
			iterator.advance();
			String key = iterator.key();
			String[] ids = key.split(BY);
			int v1 = Integer.parseInt(ids[0]);
			int v2 = Integer.parseInt(ids[1]);
			
			double weight = getPairedSim(v1, v2) * WFACTOR;
			result += weight * squareEuclidean(p_theta_d[v1], p_theta_d[v2]);
		}
		return result;
	}
	
	public double squareEuclidean(double[] v1, double[] v2) {
		double result = 0;
		if (v1.length != v2.length ) return result;
		for (int i=0; i<v1.length; i++) {
			result += (v1[i] - v2[i]) * (v1[i]-v2[i]);
		}
		return result;
	}
	
	/**
	 * Calculate the value of objective function.
	 */
	private double qFunction () {
		double q=0;
		for(int docIdx=0; docIdx<documents.size(); docIdx++) {
			for(int tokenIdx=0; tokenIdx<termIndex[docIdx].length; tokenIdx++) {
				double tq = 0;
				int term = termIndex[docIdx][tokenIdx];
				for(int topicIdx=0; topicIdx<numOfTopics; topicIdx++) {
					tq += z_w_d[docIdx][tokenIdx][topicIdx] 
					          * (Math.log(p_theta_d[docIdx][topicIdx] + EPS) +
					        	 Math.log(p_w_theta[topicIdx][term] + EPS) );
				}
				q += tf[docIdx][tokenIdx]*tq;
			}
		}
		q = (1-lambda)*q;
		double r = calculateLinkRegularization();
		System.out.println("Topic reg: " + q + ", network reg: " + r);
		q = q - lambda * r / 2 * WFACTOR;
		
		return q;
	}
	
	public void test(InstanceList testDocuments, int numIter) {
		this.testDocuments = testDocuments;
		initTestTf();
		foldingInQueries(numIter);
	}
	
	/**
	 * Iterate over test documents so that we can get topic distribution of test set.
	 * @param numIter
	 */
	public void foldingInQueries(int numIter) {
		double[][][] test_z_w_d;
		
		Randoms r = new Randoms();
		test_z_w_d = new double[testDocuments.size()][][];
		for(int docIdx = 0; docIdx < testDocuments.size(); docIdx++) {
			test_z_w_d[docIdx] = new double[testTermIndex[docIdx].length][numOfTopics];
		}
		
		test_p_theta_d = new double[testDocuments.size()][numOfTopics];
		for (int i=0; i<testDocuments.size(); i++) {
			randomInitArray(r, test_p_theta_d[i]);
		}
		for(int i=0; i<numIter; i++) {
			// E-step
			for(int docIdx=0; docIdx<testDocuments.size(); docIdx++) {
				for(int tokenIdx=0; tokenIdx<testTermIndex[docIdx].length; tokenIdx++) {
					int term = testTermIndex[docIdx][tokenIdx];
					for(int topicIdx=0; topicIdx<numOfTopics; topicIdx++) {
						test_z_w_d[docIdx][tokenIdx][topicIdx] = 
							test_p_theta_d[docIdx][topicIdx] * p_w_theta[topicIdx][term];
					}
					normalizeArray(test_z_w_d[docIdx][tokenIdx]);
				}
			}
			// M-step
			for(int j=0; j<testDocuments.size(); j++) 
				Arrays.fill(test_p_theta_d[j], 0);
			
			for (int docIdx=0; docIdx<testDocuments.size(); docIdx++) {
				for(int tokenIdx=0; tokenIdx<testTermIndex[docIdx].length; tokenIdx++) {
					int term = testTermIndex[docIdx][tokenIdx];
					int freq = testTf[docIdx][tokenIdx];
					for(int topicIdx=0; topicIdx<numOfTopics; topicIdx++) {
						double temp = (double)freq * test_z_w_d[docIdx][tokenIdx][topicIdx];
						test_p_theta_d[docIdx][topicIdx] += temp/(double)testDocLength[docIdx];
					}
				}
			}
			
			for(int j=0; j<testDocuments.size(); j++) 
				normalizeArray(test_p_theta_d[j]);
		}
	}

	private void initTestTf() {
		MalletTfidf tfidf = new MalletTfidf(testDocuments);
		this.testTf = tfidf.tf;
		this.testTermIndex = tfidf.termIndex;
		this.testDocLength = tfidf.docLength;
	}

	public void train(int numIter) {
		/* Assign spaces to needed arrays, and also randomly init them */
		initArraySpace();
		
		for(int i=0; i<10; i++) {
			expectation(p_theta_d, p_w_theta);
			basicMaximization();
		}
		
		for(int i=0; i<numIter; i++) {
			expectation(p_theta_d, p_w_theta);
			maximization();
			System.out.println("Iteration: " + i);
		}
		
	}
	
	/**
	 * E-step in EM algorithm.
	 * @param old_p_theta_d
	 * @param old_p_w_theta
	 */
	public void expectation(double[][] old_p_theta_d, double[][] old_p_w_theta) {
		for(int docIdx=0; docIdx<documents.size(); docIdx++) {
			for(int tokenIdx=0; tokenIdx<termIndex[docIdx].length; tokenIdx++) {
				int term = termIndex[docIdx][tokenIdx];
				for(int topicIdx=0; topicIdx<numOfTopics; topicIdx++) {
					z_w_d[docIdx][tokenIdx][topicIdx] = 
						old_p_w_theta[topicIdx][term] * old_p_theta_d[docIdx][topicIdx];
				}
				normalizeArray(z_w_d[docIdx][tokenIdx]);
			}
		}
	}
	
	/**
	 * M-step in EM algorithm.
	 */
	public void maximization() {
		/* M-step for p_w_theta and p_theta_d */
		basicMaximization();
//		iterationForNetwork();
	}

	private void basicMaximization() {
		for(int i=0; i<numOfTopics; i++)
			Arrays.fill(p_w_theta[i], 0);
		
		for (int i=0; i<documents.size(); i++) 
			Arrays.fill(p_theta_d[i], 0);
		
		for(int docIdx=0; docIdx<documents.size(); docIdx++) {
			for(int tokenIdx=0; tokenIdx<termIndex[docIdx].length; tokenIdx++) {
				int term = termIndex[docIdx][tokenIdx];
				int freq = tf[docIdx][tokenIdx];
				for(int topicIdx=0; topicIdx<numOfTopics; topicIdx++) {
					double temp = (double)freq * z_w_d[docIdx][tokenIdx][topicIdx];
//					p_theta_d[docIdx][topicIdx] += temp/(double)docLength[docIdx];
					p_theta_d[docIdx][topicIdx] += temp;
					p_w_theta[topicIdx][term] += temp;
				}
			}
		}		
		for(int i=0; i<numOfTopics; i++) 
			normalizeArray(p_w_theta[i]);
		
		for(int i=0; i<documents.size(); i++) 
			normalizeArray(p_theta_d[i]);
	}
	
	
	private void iterationForNetwork() {
		double[][] old_p_theta_d;
		double q_value = qFunction();
		double new_q_value = q_value ;
		int count = 0;
		while(new_q_value <= q_value) {
			q_value = new_q_value;
			old_p_theta_d = p_theta_d.clone();
			for(int vid=0; vid<documents.size(); vid++) {
				innerUpdatePThetaD(old_p_theta_d, vid);
			}
			new_q_value = qFunction();
//			System.out.println("New q value: " + new_q_value);
			count++;
			if (count % 100 == 0) {
				System.out.println("Count: " + count);
			}
		}
	}
	
	private void innerUpdatePThetaD(double[][] old_p_theta_d, int vid) {
		if (!links.get(vid).isEmpty()) {
			PairedInfo pi = links.get(vid);
			int[] pairedIds = pi.getPairedIdsArray();
			double[] sims = pi.getPairedSimArray();
			double s = arraySum(sims);
			
			s *= WFACTOR;
			
			if (s==0) return;
			for(int topicIdx = 0; topicIdx < numOfTopics; topicIdx++) {
				double linkReg = 0;
				for(int i=0; i<pairedIds.length; i++) {
					int pid = pairedIds[i];
					linkReg += sims[i] * WFACTOR * old_p_theta_d[pid][topicIdx];
				}
				linkReg /= s;
				p_theta_d[vid][topicIdx] = (1-gamma) * old_p_theta_d[vid][topicIdx] 
				                                  + gamma * linkReg; 
			}
			
		}
	}
	
	public void printTopWords (int numWords, boolean useNewLines) {
		class WordProb implements Comparable {
			int wi;
			double p;
			public WordProb (int wi, double p) { this.wi = wi; this.p = p; }
			public final int compareTo (Object o2) {
				if (p > ((WordProb)o2).p)
					return -1;
				else if (p == ((WordProb)o2).p)
					return 0;
				else return 1;
			}
		}

		WordProb[] wp = new WordProb[numOfTerms];
		for (int ti = 0; ti < numOfTopics; ti++) {
			for (int wi = 0; wi < numOfTerms; wi++)
				wp[wi] = new WordProb (wi, p_w_theta[ti][wi]);
			Arrays.sort (wp);
			if (useNewLines) {
				System.out.println ("\nTopic "+ti);
				for (int i = 0; i < numWords; i++)
					System.out.println (documents.getDataAlphabet().lookupObject(wp[i].wi).toString() + " " + wp[i].p);
			} else {
				System.out.print ("Topic "+ti+": ");
				for (int i = 0; i < numWords; i++)
					System.out.print (documents.getDataAlphabet().lookupObject(wp[i].wi).toString() + " ");
				System.out.println();
			}
		}
	}
	
	public static void main(String[] args) {
		String malletFile = "dataset/vlc/vlc_train.en.f8.mallet";
		String testMalletFile = "dataset/vlc/vlc_test.en.f8.mallet";
//		String testMalletFile = "dataset/vlc/vlc_test.title.f2.mallet";
//		String malletFile = "dataset/vlc/vlc_train.title.f2.mallet";
		String simFile = "dataset/vlc/sim_0p_10n.csv";
//		String malletFile = "dataset/cora/cora.mallet";
//		String simFile = "dataset/cora/links.train.txt";
		double lambda = 0.1;
		double gamma = 0.01;
		int numOfTopics = 8;
		NetPLSA netPlsa = new NetPLSA(numOfTopics, lambda, gamma, malletFile, simFile);
		netPlsa.train(50);
		netPlsa.printTopWords(10, true);
		InstanceList testDocuments = InstanceList.load(new File(testMalletFile));
		netPlsa.test(testDocuments, 20);
	}
}
