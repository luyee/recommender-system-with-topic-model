/**
 * This is the implementation of basic rtm as described in Chang's paper.
 * Notice I'm using the same implementation in R implementation of R package.
 * @author Haibin
 * 01-25-2012
 */
package rs.topics.model;

import java.io.*;
import java.util.*;

import gnu.trove.iterator.*;
import gnu.trove.map.hash.*;

import cc.mallet.types.*;
import cc.mallet.util.Randoms;

import rs.topics.recommender.MalletTfidf;
import rs.types.*;
import rs.util.vlc.Task1Solution;

public class PrimeRtm extends MalletTfidf{
	private static final long serialVersionUID = 1L;
	public final static String BY = "x"; 
	public final static int LIST_SIZE = 30;
	public final static int TIMES = 20;

//	public InstanceList documents;
	public TObjectIntHashMap<String> idHash;
	public TObjectIntHashMap<String> pairIdHash; 	// v1xv2 -> int

	public ArrayList<PairedInfo> links;
	public int numIterations;
	public int numOfTopics;
//	public int numOfTerms;
	
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
	public double[][] x;				// value for regression
	public double[] eta;
	
	public double lambda;			// regularization parameter

	public static final int testIndexStart = Task1Solution.testIndexStart;
	
	public PrimeRtm(int numTopics) {
		this(numTopics, 50.0, 0.01);
	}
	
	public PrimeRtm(int numTopics, double alphaSum, double beta) {
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
		if (pairIdHash.containsKey(pairIdString(v1, v2)) )
//				|| pairIdHash.containsKey(pairIdString(v2, v1)) ) 
			return true;
		else 
			return false;
	}
	
	public void estimate2(int numIterations, Randoms r, int sampleIter) {
		if (documents == null || links == null) 
			return;
		
		this.vAlpha = alpha*numOfTopics;
		this.tBeta = beta * numOfTerms;
		eta = new double[numOfTopics]; 
		zbar = new double[documents.size()][numOfTopics];
		x = new double[numOfLinks][numOfTopics];

		Arrays.fill(eta, 3);
		gibbsSampling2(r, sampleIter);
		params();

		for(int iter=1; iter<numIterations; iter++) {
			gibbsSampling2(r, sampleIter);
			params();
			if (iter > 0 ) {
				if (iter%50 == 0) {
					System.out.println();
				}
			}
			if (iter%10 == 0) {
				System.out.print(iter);				
			} else {
				System.out.print(".");
			}
		}		
	}
	
	public void estimate(int numIterations, Randoms r, int sampleIter) {
		if (documents == null || links == null) 
			return;
		numOfTerms = documents.getAlphabet().size();
		int numDoc = documents.size();
		this.vAlpha = alpha*numOfTopics;
		this.tBeta = beta * numOfTerms;
		
		topics = new int[numDoc][];
		docTopicCounts = new int[numDoc][numOfTopics];
		zbar = new double[numDoc][numOfTopics];
		x = new double[numOfLinks][numOfTopics];
		termTopicCounts = new int[numOfTerms][numOfTopics];
		topicTokenCounts = new int[numOfTopics];
		eta = new double[numOfTopics]; 
		
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
		params();
//		Arrays.fill(eta, 3);

		for(int iter=0; iter<numIterations; iter++) {
			gibbsSampling(r, sampleIter);
			params();
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
			zbar[di][ti] = (docTopicCounts[di][ti]+alpha) / ((double)docLen + alpha * numOfTopics);
			if(zbar[di][ti] > 1) {
				System.err.println("zbar > 1, " + di + ", " + ti);
			}
		}
	}
	
	
	private void _calculateLinkProbs(int di, double[] link_probs) {
		// TODO Auto-generated method stub
		int ll = links.get(di).getLength();
		Arrays.fill(link_probs, 1);
		FeatureSequence fs = (FeatureSequence)documents.get(di).getData();
		int docLen = fs.getLength();
		if (ll != 0) {
			int[] pairedDocs = links.get(di).getPairedIdsArray();
			for(int li=0; li<pairedDocs.length; li++) {
				fs = (FeatureSequence)documents.get(pairedDocs[li]).getData();
				int ddLen = fs.getLength();
				for(int k=0; k<numOfTopics; k++) {
					link_probs[k] *= Math.exp(eta[k] * docTopicCounts[pairedDocs[li]][k] / (double)docLen / (double)ddLen);
				}
			}
		}
		
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
		if (!links.get(v1).isEmpty())
			return links.get(v1).getSim(v2);
		return 0;
	}
	
	/**
	 * This sampling method is different in the sense each same term 
	 * would be assigned to the same topic, that's why we need tf-idf
	 * here. 
	 */
	public void gibbsSampling2(Randoms r, int sampleIter) {
		numOfTerms = documents.getAlphabet().size();
		int numDoc = documents.size();
		topics = new int[numDoc][];
		docTopicCounts = new int[numDoc][numOfTopics];
		termTopicCounts = new int[numOfTerms][numOfTopics];
		topicTokenCounts = new int[numOfTopics];
		
		for(int di=0; di<numDoc; di++) {
			topics[di] = new int[this.tf[di].length];
		}
		
		int oldTopic, newTopic, term;
		double[] topicWeights = new double[this.numOfTopics];
		double tw;
		double topicWeightsSum;
		int docLen;
		double[] link_probs = new double[numOfTopics];
		for(int ii=0; ii<sampleIter; ii++) {
			for(int di=0; di<documents.size(); di++) {
				FeatureSequence fs = (FeatureSequence)documents.get(di).getData();
				docLen = fs.getLength();
				
				
				/* Calculate link probability for this document */
				if( ii > 0)
					_calculateLinkProbs(di, link_probs);
				for(int si=0; si<this.termIndex[di].length; si++) {
					term = termIndex[di][si];
					int count = this.tf[di][si];
					oldTopic = topics[di][si];
					if(ii>0) {
						docTopicCounts[di][oldTopic] -= count;
						termTopicCounts[term][oldTopic] -= count;
						topicTokenCounts[oldTopic] -= count;
					}
					Arrays.fill(topicWeights, 0);
					topicWeightsSum = 0;
					
					for(int ti=0; ti<this.numOfTopics; ti++) {
						if(ii > 0) {
							tw = link_probs[ti] * (docTopicCounts[di][ti] + alpha) * (termTopicCounts[term][ti] + beta) 
								/ (topicTokenCounts[ti] + tBeta);
						} else {
							tw = 1.0;
						}
						topicWeightsSum += tw;
						topicWeights[ti] = tw;
					}
					newTopic = r.nextDiscrete(topicWeights, topicWeightsSum);
					topics[di][si] = newTopic;
					docTopicCounts[di][newTopic] += count;
					termTopicCounts[term][newTopic] += count;
					topicTokenCounts[newTopic] += count;
				}
				/* Update zbar values */
				_calculateZbar(di, docLen);
			}
		}
	}

	public void gibbsSampling(Randoms r, int sampleIter) {
		int oldTopic, newTopic, term;
		double[] topicWeights = new double[this.numOfTopics];
		double tw;
		double topicWeightsSum;
		int docLen;
		double[] link_probs = new double[numOfTopics];
		
		for(int ii=0; ii<sampleIter; ii++) {
			for(int di=0; di<documents.size(); di++) {
				FeatureSequence fs = (FeatureSequence)documents.get(di).getData();
				docLen = fs.getLength();
				
				/* Calculate link probability for this document */
				_calculateLinkProbs(di, link_probs);
				
				for (int si=0; si<docLen; si++) {
					term = fs.getIndexAtPosition(si);
					oldTopic = topics[di][si];
					docTopicCounts[di][oldTopic]--;
					termTopicCounts[term][oldTopic]--;
					topicTokenCounts[oldTopic]--;	
					Arrays.fill(topicWeights, 0);
					topicWeightsSum = 0;
					
					for(int ti=0; ti<this.numOfTopics; ti++) {
						tw = link_probs[ti] * (docTopicCounts[di][ti] + alpha) * (termTopicCounts[term][ti] + beta) 
								/ (topicTokenCounts[ti] + tBeta);
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
	


	public void initFromFile(String trainMalletFile, String linkFile) throws IOException {
		documents = InstanceList.load(new File(trainMalletFile));
		
		initTfidf();
//		documents = new LectureMalletImporter().readCsvFile("dataset/vlc_train.complete.txt");
//		InstanceList testDocuments = InstanceList.load(new File(testMalletFile));
		initIdHash();
		readLinksIntoMemory(linkFile);
		
		/* Initialize lambda */
		lambda = (double)numOfLinks /(double) (documents.size() * (documents.size() -1)/2);
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
	 * Calculate the eta value.
	 */
	public void params() {
		for(int di=0; di<documents.size(); di++) {
			FeatureSequence fs = (FeatureSequence) documents.get(di).getData();
			int docLen = fs.getLength();
			_calculateZbar(di, docLen);
		}
		_calculateXValue();
		double[] p = new double[this.numOfTopics];
		Arrays.fill(p, 0);
		for(int i=0; i<x.length; i++) {
			for(int t=0; t<numOfTopics; t++) {
				p[t] += x[i][t];
			}
		}
		for(int t=0; t<numOfTopics; t++) {
			double temp = p[t] / (p[t] + lambda * alpha * alpha * documents.size() * (documents.size() -1) /2);
			eta[t] = Math.log(temp/(1-temp));
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
//		pairIds = new ArrayList<String>();
		
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
				numOfLinks++;
			} else {
				System.err.println("Duplicated pair:" + fields[0] + ", " + fields[1] );
			}
			
			this.addPair(doc1, doc2, sim);
			this.addPair(doc2, doc1, sim);
		}
		reader.close();
//		System.out.println("Num of links: " + numOfLinks);
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
		int numOfTopic = 8;
		int numIter = 30;
		int sampleIter = 100;
		double alpha = 0.1;
		double beta = 0.1;
		
//		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
		String malletFile = "dataset/cora/cora.mallet";
		String simFile = "dataset/cora/links.txt";
		PrimeRtm rtm = new PrimeRtm(numOfTopic, alpha*numOfTopic, beta);
		rtm.initFromFile(malletFile, simFile);
		rtm.estimate(numIter, new Randoms(), sampleIter);
		rtm.printTopWords(10, false);
	}
}

