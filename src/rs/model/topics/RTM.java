/**
 * In order to improve its efficiency, we calculate the link factor for a document only 
 * once in this model.
 */
package rs.model.topics;

import java.io.*;
import java.util.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import org.apache.commons.math.stat.regression.*;

import cc.mallet.types.*;
import cc.mallet.util.Randoms;

import rs.types.*;
import rs.util.vlc.solution.Task1Solution;

public class RTM extends PairData {
	public final static int LIST_SIZE = 30;
	public final static int TIMES = 20;

	public int numIterations;
	public int numOfTopics;
	public int numTerms;
	
	public double alpha;
	public double vAlpha;				// sum over all alpha
	public double beta;
	
	public double tBeta;				// sum over all beta
	public int[][] topics;				// record which topic a word belongs to in each iteration
	public int[][] docTopicCounts; 	// indexed by doc, topic	
	public int[][] termTopicCounts;	// indexed by term, topic
	public int[] topicTokenCounts;		// indexed by topic, meaning how many tokens in each topic
	
	public double[][] zbar;			// indexed by doc, topic, average doc distribution of documents
	public double[] y;					// value of normalized pair feature
	public double[][] x;				// value for regression
	public double[] eta;
	
	/* f1, f2, and f3 are three factors in calculation of relational topic model.
	 * Indexed by doc, topic */
	public double[][] f1;
	public double[][] f2;
	
	/**
	 * Relational factor of the power value.
	 */
	public double[][] rFactor;
	
	public double sigma;
	
	public static final int testIndexStart = Task1Solution.testIndexStart;
	
	public RTM(int numTopics) {
		this(numTopics, 50.0, 0.01);
	}
	
	public RTM(int numTopics, double alphaSum, double beta) {
		this.numOfTopics = numTopics;
		this.alpha = alphaSum/numOfTopics;
		this.beta = beta;
	}

	/**
	 * Calculate the value of rFactor in sampling.
	 * @param di
	 * @param docLen
	 */
	private void _calculateRFactorForADoc(int di, int docLen) {
		int ll = links.get(di).getLength();
		Arrays.fill(rFactor[di], 1);
		if (ll != 0) {
			int[] pairedDocs = links.get(di).getPairedIdsArray();
			double f3 = 0; //
			for(int i=0; i<ll; i++) {
				int dii = pairedDocs[i];
				for(int k=0; k<numOfTopics; k++) {
					f3 += eta[k] * zbar[di][k] * zbar[dii][k];
				}
			}
			double f1 = 0, f2 = 0;
			for(int k=0; k<numOfTopics; k++) {
				for (int i=0; i<ll; i++) {
					int dii = pairedDocs[i];
					f1 += getPairedSim(di, dii) * zbar[dii][k];
					f2 += (eta[k]*zbar[dii][k] / (double)docLen) * (eta[k]*zbar[dii][k] / (double)docLen + 2*f3);
				}
				f1 = f1 * 2 * eta[k]/(double)docLen;
				rFactor[di][k] = Math.exp(f1-f2);
			}
		}
	}
	
	private void _calculateRFactorForDocs() {
		for(int i=0; i<documents.size(); i++) {
			FeatureSequence fs = (FeatureSequence)documents.get(i).getData();
			_calculateRFactorForADoc(i, fs.getLength());
		}
	}
	
	/**
	 * Calculate values of x for regression. X is calculated based on zbar, and it must be 
	 * in the same order as y array.
	 * @param zbar
	 * @param x
	 * @return x
	 */
	private void _calculateXValue() {
		TObjectIntIterator<String> iterator = pairIdHash.iterator();
		while(iterator.hasNext()) {
			iterator.advance();
			String key = iterator.key();
			String[] ids = key.split(BY);
			int v1 = Integer.parseInt(ids[0]);
			int v2 = Integer.parseInt(ids[1]);
			int docIdx = iterator.value();
			for(int i=0; i<x[docIdx].length; i++) {
				x[docIdx][i] = zbar[v1][i] * zbar[v2][i];
			}
		}
	}
	
	public void estimate(int numIterations, Randoms r) {
		if (documents == null || links == null) 
			return;
		int sampleIter = 20;
		numTerms = documents.getAlphabet().size();
		int numDoc = documents.size();
		this.vAlpha = alpha*numOfTopics;
		this.tBeta = beta * numTerms;
		
		topics = new int[numDoc][];
		docTopicCounts = new int[numDoc][numOfTopics];
		zbar = new double[numDoc][numOfTopics];
		x = new double[numOfLinks][numOfTopics];
		termTopicCounts = new int[numTerms][numOfTopics];
		topicTokenCounts = new int[numOfTopics];
		eta = new double[numOfTopics]; 	// the first value is intercept
		rFactor = new double[numDoc][numOfTopics];
		
		for(int di=0; di<numDoc; di++) {
			FeatureSequence fs = (FeatureSequence) documents.get(di).getData();
			int docLen = fs.getLength();
			topics[di] = new int[docLen];
			for(int si=0; si<docLen; si++) {
				int topic = r.nextInt(numOfTopics);
				topics[di][si] = topic;
				docTopicCounts[di][topic]++;
				topicTokenCounts[topic]++;
				termTopicCounts[fs.getIndexAtPosition(si)][topic]++;
			}
			_calculateZbar(di, docLen);
		}
		_calculateXValue();
		params(y, x);

		for(int iter=0; iter<numIterations; iter++) {
			gibbsSampling(r, sampleIter);
			_calculateXValue();
			params(y, x);
			if (iter > 0 ) {
				if (iter%50 == 0) {
					System.out.println();
//					printRegressionParameters();
//					printTopWords (5, false);
				}
			}
			if (iter%10 == 0) {
				System.out.print(iter);				
			} else {
				System.out.print(".");
			}
		}		
	}

	private void _calculateZbar(int di, int docLen) {
		for(int ti=0; ti<numOfTopics; ti++) {
			zbar[di][ti] = ((double)docTopicCounts[di][ti]+alpha) / ((double)docLen + alpha);
		}
	}
	
	public void printRegressionParameters() {
		System.out.println("Coefficients: ");
		for(int i=0; i<this.eta.length; i++) {
			System.out.println(i + ": " + this.eta[i]);
		}
		System.out.println("sigma: " + this.sigma);
	}
	
	public InstanceList getDocuments() {
		return documents;
	}
	
	private void gibbsSampling(Randoms r, int sampleIter) {
		int oldTopic, newTopic, term;
		double[] topicWeights = new double[this.numOfTopics];
		double tw;
		double topicWeightsSum;
		int docLen;
		for(int ii=0; ii<sampleIter; ii++) {
			for(int di=0; di<documents.size(); di++) {
				FeatureSequence fs = (FeatureSequence)documents.get(di).getData();
				docLen = fs.getLength();
				/* Calculate link probability for this document */
				_calculateRFactorForADoc(di, docLen);
				for (int si=0; si<docLen; si++) {
					term = fs.getIndexAtPosition(si);
					oldTopic = topics[di][si];
					docTopicCounts[di][oldTopic]--;
					termTopicCounts[term][oldTopic]--;
					topicTokenCounts[oldTopic]--;	
					Arrays.fill(topicWeights, 0);
					topicWeightsSum = 0;
					
					for(int ti=0; ti<this.numOfTopics; ti++) {
						tw = (docTopicCounts[di][ti] + alpha) * (termTopicCounts[term][ti] + beta) 
								/ (topicTokenCounts[ti] + tBeta) * rFactor[di][ti];
						topicWeightsSum += tw;
						topicWeights[ti] = tw;
					}
					newTopic = r.nextDiscrete(topicWeights, topicWeightsSum);
					topics[di][si] = newTopic;
					docTopicCounts[di][newTopic]++;
					termTopicCounts[term][newTopic]++;
					topicTokenCounts[newTopic]++;
				}
				/* Update zbar values */
				_calculateZbar(di, docLen);
			}
		}
	}
	
	
	/**
	 * Import instance list from serialized object file.
	 * @param file
	 */
	public void loadInstanceList(String file) {
		this.documents = InstanceList.load(new File(file));
	}
	
	/**
	 * Carry out linear regression based on y and x value
	 * @param y
	 * @param x
	 * @param eta Eta values will be set using this array.
	 * @return Variance value sigma
	 */
	public void params(double[] y, double[][] x) {
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		regression.setNoIntercept(true);
		
//		int end = 6011;
//		double[] newY = Arrays.copyOf(y, end);
//		double[][] newX = new double[end][];
//		for(int i=0; i<newX.length; i++) {
//			newX[i] = Arrays.copyOf(x[i], x[i].length);
//		}
//		System.out.println(pairIds.get(end-1));
//		String[] vids = pairIds.get(end-1).split(BY);
//		
//		System.out.println(documents.get(Integer.parseInt(vids[0])).getName());
//		System.out.println(documents.get(Integer.parseInt(vids[1])).getName());
//		Instance inst1 = documents.get(Integer.parseInt(vids[0]));
//		Instance inst2 = documents.get(Integer.parseInt(vids[1]));
//		double[] zbarInst1 = zbar[Integer.parseInt(vids[0])];
//		double[] zbarInst2 = zbar[Integer.parseInt(vids[1])];
//		regression.newSampleData(newY, newX);
		
		regression.newSampleData(y, x);
		this.eta = regression.estimateRegressionParameters();
		this.sigma = regression.estimateErrorVariance();
	}
	

	public void printTopWords (int numWords, boolean useNewLines)
	{
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

		WordProb[] wp = new WordProb[numTerms];
		for (int ti = 0; ti < numOfTopics; ti++) {
			for (int wi = 0; wi < numTerms; wi++)
				wp[wi] = new WordProb (wi, ((double)termTopicCounts[wi][ti]) / topicTokenCounts[ti]);
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
	
	/**
	 * Add test documents into training documents to train together.
	 * @param testDocuments
	 */
	public void addTestInstanceList(InstanceList testDocuments) {
//		this.testIndexStart = documents.size(); 	// from this index are test cases
		Iterator<Instance> iterator = testDocuments.iterator();
		while(iterator.hasNext()) {
			documents.add(iterator.next());
		}
	}

	
	public static void main(String[] args) throws IOException {
		int numOfTopic = 40;
		int numIter = 100;
		double alpha = 0.0016;
		double beta = 0.0048;
		
		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
		String simFile = "dataset/vlc/sim5p.csv";
		String solutionFile = "dataset/vlc/task1_solution.en.f8.lm.txt";
		String queryFile = "dataset/task1_query.en.f8.txt";
		String targetFile = "dataset/task1_target.en.f8.txt";
		
		if (args.length >= 2) {
			numOfTopic = Integer.parseInt(args[0]);
			numIter = Integer.parseInt(args[1]);
		}
		if (args.length >= 5) {
			malletFile = args[2];
			solutionFile = args[3];
			queryFile = args[4];
		}
		System.out.println("Number of topics: " + numOfTopic + ", number of iteration: " + numIter);
		/* test */
		long start = System.currentTimeMillis();
		RTM rtm = new RTM(numOfTopic, alpha*numOfTopic, beta);
		rtm.initFromFile(malletFile, simFile);
	
		Randoms r = new Randoms();
		rtm.estimate(numIter, r);
		System.out.println("document size:" + rtm.documents.size());
		System.out.println("Link document length: " + rtm.links.size());
		System.out.println("link size:" + rtm.numOfLinks);
		System.out.println("Pair size: " + rtm.pairIdHash.size());
		
		int ps = 0;
		Iterator<PairedInfo> iterator = rtm.links.iterator();
		while(iterator.hasNext()) {
			PairedInfo pi = iterator.next();
			ps += pi.getLength();
		}
		System.out.println("Size of paired links: " + ps);

		System.out.println("Time cost: " + (System.currentTimeMillis() - start));
		rtm.printTopWords (10, true);
//		ArrayList<String> solution = rtm.query(queryFile);
//		BufferedWriter writer = new BufferedWriter(new FileWriter(solutionFile));
//		for(int i=0; i<solution.size(); i++) {
//			writer.write(solution.get(i));
//			writer.newLine();
//		}
//		writer.flush();
//		writer.close();
//		StringBuffer sb = new StringBuffer();
//		sb.append("rtm.");
//		sb.append(numOfTopic);
//		sb.append(".");
//		sb.append(numIter);
//		sb.append(".dat");
//		ObjectOutputStream obj_out = new ObjectOutputStream(new FileOutputStream(sb.toString()));
//		obj_out.writeObject(rtm);
//		obj_out.flush();
//		obj_out.close();
	}
}
