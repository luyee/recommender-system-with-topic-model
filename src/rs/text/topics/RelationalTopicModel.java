/**
 * This model calculates link factor for every time when it samples over a term.
 * Different from the model RTM, which only calculates the factor once for 
 * a document.
 */
package rs.text.topics;

import java.io.*;
import java.util.*;

import gnu.trove.iterator.*;
import gnu.trove.list.array.*;
import gnu.trove.map.hash.*;
import org.apache.commons.math.stat.regression.*;

import cc.mallet.types.*;
import cc.mallet.util.Randoms;

import rs.types.*;
import rs.util.vlc.Task1Solution;

public class RelationalTopicModel implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	public final static String BY = "x"; 
	public final static int LIST_SIZE = 30;
	public final static int TIMES = 20;

	public InstanceList documents;
	public TObjectIntHashMap<String> idHash;
	public TObjectIntHashMap<String> pairIdHash; 	// v1xv2 -> int

	public ArrayList<PairedInfo> links;
	public int numIterations;
	public int numOfTopics;
	public int numTerms;
	
	public int numOfLinks;
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
	
	public RelationalTopicModel(int numTopics) {
		this(numTopics, 50.0, 0.01);
	}
	
	public RelationalTopicModel(int numTopics, double alphaSum, double beta) {
		this.numOfTopics = numTopics;
		this.alpha = alphaSum/numOfTopics;
		this.beta = beta;
	}
	
	public void addPair(int doc1, int doc2, double sim) {
		PairedInfo p = links.get(doc1);
		if (p == null) {
			p = new PairedInfo(doc1);
		}
		p.add(doc2, sim);
	}
	
	/**
	 * Calculate the factor of paired links for the process of drawing a new 
	 * topic for a token in Gibbs sampling  
	 * 
	 * @param k topic we need to calculate weight.
	 * @param di document we are sampling.
	 * @param si token we are sampling
	 * @param term term of the sampled token
	 * @param docLen Length of specified document
	 * @return
	 */
	private double _calculateLinkFactor(int k, int di, int si, int docLen) {
		if (this.sigma == 0) return 0;
		double f1, f2, f3;
		f1 = f2 = f3 = 0;
		int ll = links.get(di).getLength();
		if (ll != 0) {
			int[] pairedDocs = links.get(di).getPairedIdsArray();
			for (int i=0; i<ll; i++) {
				int dii = pairedDocs[i];
				f1 += this.getPairedSim(di, dii) * zbar[dii][k];
				f2 += (double)(eta[k]*zbar[dii][k])*(double)(eta[k]*zbar[dii][k]) /(double)(docLen*docLen);
				double tmp = 0;
				for(int ki=0; ki<numOfTopics; ki++) {
					tmp += eta[ki]*zbar[di][ki]*zbar[dii][ki];
				}
				f3 += (double)2*eta[k]*zbar[dii][k]*tmp/(double)docLen;
			}
			f1 = f1*2*eta[k]/(double)docLen;
		}
		
		double result = f1 - f2 + f3 /(double)(2*this.sigma);
		return result;
	}
	
	private double _calculateLinkFactor2(int k, int di, int si, int docLen) {
		if (this.sigma == 0) return 0;
		
		double f3 = 0;
		int ll = links.get(di).getLength();
		if (ll != 0) {
			int[] pairedDocs = links.get(di).getPairedIdsArray();
			for (int i=0; i<ll; i++) {
				int dii = pairedDocs[i];
				double tmp = 0;
				for(int ki=0; ki<numOfTopics; ki++) {
					tmp += eta[ki]*zbar[di][ki]*zbar[dii][ki];
				}
				f3 += (double)2*eta[k]*zbar[dii][k]*tmp/(double)docLen;
			}
		}
		double result = f1[di][k] - f2[di][k] + f3 /(double) (2*this.sigma);
		return result;
	}
	
	/* This method should be called once before sampling a document.
	 * 
	 */
	private void _calculateF1F2ForADoc(int di, int docLen) {
		int ll = links.get(di).getLength();
		if (ll != 0) {
			int[] pairedDocs = links.get(di).getPairedIdsArray();
			Arrays.fill(f1[di], 0);
			Arrays.fill(f2[di], 0);
			for (int k=0; k<numOfTopics; k++) {
				for (int i=0; i<ll; i++) {
					int dii = pairedDocs[i];
					f1[di][k] += this.getPairedSim(di, dii) * zbar[dii][k];
					f2[di][k] += (double)(eta[k]*zbar[dii][k])*(double)(eta[k]*zbar[dii][k]);
				}
				f1[di][k] = f1[di][k]*2*eta[k]/(double)docLen;
				f2[di][k] = f2[di][k]/(double)(docLen*docLen);
			}
		}
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
	
	public boolean containsPair(int v1, int v2) {
		if (pairIdHash.containsKey(pairIdString(v1, v2)) ||
				pairIdHash.containsKey(pairIdString(v2, v1)) ) 
			return true;
		else 
			return false;
	}
	
	public void estimate(int numIterations, Randoms r) {
		if (documents == null || links == null) 
			return;
		
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
		
		f1 = new double[numDoc][numOfTopics];
		f2 = new double[numDoc][numOfTopics];
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
			for(int ti=0; ti<numOfTopics; ti++) {
				zbar[di][ti] = (double)docTopicCounts[di][ti] / (double)docLen;
			}
		}
		_calculateXValue();
		params(y, x);
//		_calculateRFactorForDocs();
		for(int di=0; di<numDoc; di++) {
			FeatureSequence fs = (FeatureSequence) documents.get(di).getData();
			int docLen = fs.getLength();
			_calculateF1F2ForADoc(di, docLen);
		}

		for(int iter=0; iter<numIterations; iter++) {
			gibbsSampling(r);
			_calculateXValue();
			params(y, x);
//			_calculateRFactorForDocs();
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
	
	public ArrayList<PairedInfo> getLinks() {
		return links;
	}
	
	public int getNumOfLinks() {
		return this.numOfLinks;
	}
	
	/**
	 * Return the sim value of a specified pair. 
	 * 
	 * @param v1
	 * @param v2
	 * @return Less than 0 means this pair does not exist.
	 */
	public double getPairedSim(int v1, int v2) {
		if (links.get(v1) != null)
			return links.get(v1).getSim(v2);
		return -1;
	}

	private void gibbsSampling(Randoms r) {
		int oldTopic, newTopic, term;
		double[] topicWeights = new double[this.numOfTopics];
		double tw, factor;
		double topicWeightsSum;
		int docLen;
		for(int di=0; di<documents.size(); di++) {
			FeatureSequence fs = (FeatureSequence)documents.get(di).getData();
			docLen = fs.getLength();
			_calculateF1F2ForADoc(di, docLen);
			
			for (int si=0; si<docLen; si++) {
				term = fs.getIndexAtPosition(si);
				oldTopic = topics[di][si];
				docTopicCounts[di][oldTopic]--;
				termTopicCounts[term][oldTopic]--;
				topicTokenCounts[oldTopic]--;	
				Arrays.fill(topicWeights, 0);
				topicWeightsSum = 0;
				zbar[di][oldTopic] = docTopicCounts[di][oldTopic]/docLen;	
				
				for(int ti=0; ti<this.numOfTopics; ti++) {
//					factor = _calculateLinkFactor(ti, di, si, docLen);
					factor = _calculateLinkFactor2(ti, di, si, docLen);
//					factor = rFactor[di][ti];
					tw = (docTopicCounts[di][ti] + alpha) * (termTopicCounts[term][ti] + beta) 
							/ (topicTokenCounts[ti] + tBeta) * Math.exp(factor); //* factor;  
					topicWeightsSum += tw;
					topicWeights[ti] = tw;
				}
				newTopic = r.nextDiscrete(topicWeights, topicWeightsSum);
				topics[di][si] = newTopic;
				docTopicCounts[di][newTopic]++;
				termTopicCounts[term][newTopic]++;
				topicTokenCounts[newTopic]++;
				zbar[di][newTopic] = (double)docTopicCounts[di][newTopic]/(double)docLen;
			}
		}
	}
	
	public void initFromFile(String trainMalletFile, String linkFile) throws IOException {
		documents = InstanceList.load(new File(trainMalletFile));
//		documents = new LectureMalletImporter().readCsvFile("dataset/vlc_train.complete.txt");
//		InstanceList testDocuments = InstanceList.load(new File(testMalletFile));
		initIdHash();
		readLinksIntoMemory(linkFile);
	}
	
	/**
	 * Init idHahs according to documents, here it is an instance list.
	 */
	private void initIdHash() {
		if (documents == null) {
			System.err.println("Document instance should be initialized first.");
			return;
		}
		if(idHash == null) {
			idHash = new TObjectIntHashMap<String>();
		}
		for(int i=0; i<documents.size(); i++) {
			Instance doc = documents.get(i);
			String vId = (String) doc.getName();
			idHash.put(vId, i);
		}
	}
	
	/**
	 * Import instance list from serialized object file.
	 * @param file
	 */
	public void loadInstanceList(String file) {
		this.documents = InstanceList.load(new File(file));
	}
	
	private String pairIdString(int v1, int v2) {
		return Integer.toString(v1) + BY + Integer.toString(v2);
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
	 * Import links data into system. Here link means co-viewed pair info, 
	 * including both id and frequency.
	 * @param linkFile This is the file with normalized similarity values.
	 * @throws IOException 
	 */
	public void readLinksIntoMemory(String linkFile) throws IOException {
		if (links == null) {
			links = new ArrayList<PairedInfo>(documents.size());
			for(int i=0; i<documents.size(); i++) {
				links.add(new PairedInfo(i));
			}
		}
		pairIdHash = new TObjectIntHashMap<String>();
		TDoubleArrayList simArray = new TDoubleArrayList();
		
		BufferedReader reader = new BufferedReader(
				new FileReader(linkFile));
		String s;
		
		while( (s=reader.readLine()) != null) {
			if (s.trim().startsWith("#")) continue;
			String[] fields = s.split("\\s*,\\s*");
			if (fields.length != 3) continue;
			int doc1, doc2;
			double sim;
			
			if ( !(idHash.containsKey(fields[0].trim())
					&& idHash.containsKey(fields[1].trim())) ) {
//				System.out.println(fields[0] + "," + fields[1]);
				continue;
			}
			doc1 = idHash.get(fields[0].trim());
			doc2 = idHash.get(fields[1].trim());
			sim = Double.parseDouble(fields[2]);
			
			if( !this.containsPair(doc1, doc2)) {
				pairIdHash.put(pairIdString(doc1, doc2), numOfLinks);
				simArray.add(sim);
				numOfLinks++;
			} else {
				System.err.println("Duplicated pair:" + fields[0] + ", " + fields[1] + fields[2]);
			}
			
			this.addPair(doc1, doc2, sim);
			this.addPair(doc2, doc1, sim);
		}
		reader.close();
		y = simArray.toArray();
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
		int numIter = 500;
		double alpha = 0.0016;
		double beta = 0.0048;
		
		String solutionFile = "dataset/task1_solution.txt";
		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
		String queryFile = "dataset/task1_query.csv";
		String simFile = "dataset/sim.csv";
		
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
		RelationalTopicModel rtm = new RelationalTopicModel(numOfTopic, alpha*numOfTopic, beta);
		rtm.initFromFile(malletFile, simFile);
		System.out.println("document size:" + rtm.documents.size());
		System.out.println("link size:" + rtm.numOfLinks);

		Randoms r = new Randoms();
		rtm.estimate(numIter, r);
		System.out.println("Link document length: " + rtm.links.size());
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
