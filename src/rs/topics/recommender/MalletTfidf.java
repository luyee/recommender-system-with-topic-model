/**
 * This class tries to get tf-idf features based on mallet model. 
 * Because mallet already has the feature sequence, we can easily 
 * make use of that list. Furthermore, I think it may be necessary 
 * to do some further pre-processing so that punctuations and too 
 * short words should be eliminated.
 */
package rs.topics.recommender;

import java.io.*;
import java.util.Arrays;

import cc.mallet.types.*;

import rs.topics.model.RelationalTopicModel;
import rs.util.vlc.Task1Solution;

public class MalletTfidf implements Serializable {
	public int[][] termIndex;		// indexed by <document, term>, this indicates which terms appear in each doc
	public int[][] tf;				// indexed by <document, term>, indicating frequency of each term
	public int[] df;				// document frequency for each term
	public double[] idf;			// inverse document frequency for each term
	public double[][] tfidf;		// tfidf for each term in document, indexed by <document, term>
	public int[] docLength;
	public int numOfTerms;
	public int numOfDocs;
	
	public static final int testIndexStart = RelationalTopicModel.testIndexStart;

	public InstanceList documents;
	public Task1Solution solver;
		
	public MalletTfidf(InstanceList doc) {
		this.documents = doc;		
		initTfidf();
	}
	
	public void initSolver() {
		solver = new Task1Solution(documents) {
			public String recommend(String qId) {
				int qdocId = idHash.get(qId);
				
				int test_size = documents.size() - testIndexStart;
				double[] predSim = new double[test_size];
				Arrays.fill(predSim, 0);
				for(int i=0; i<test_size; i++) {
					predSim[i] = getTfidfSim(qdocId, testIndexStart+i);		
				}
				String line = sortRecommendList(qdocId, predSim);
				return line;
			}
		};
	}
	
	public void initTfidf() {
		numOfDocs = documents.size();
		numOfTerms = documents.getDataAlphabet().size();
		idf = new double[numOfTerms];
		df = new int[numOfTerms];
		termIndex = new int[documents.size()][];
		tf = new int[documents.size()][];
		tfidf = new double[documents.size()][];
		docLength = new int[documents.size()];
		
		Arrays.fill(df, 0);
		
		for(int docIdx=0; docIdx < documents.size(); docIdx++) {
			FeatureSequence fs = (FeatureSequence)documents.get(docIdx).getData();
			docLength[docIdx] = fs.getLength();
			int[] tokens = fs.toSortedFeatureIndexSequence();
			int termNum = distinctNumbers(tokens);
			termIndex[docIdx] = new int[termNum];
			tf[docIdx] = new int[termNum];
			tfidf[docIdx] = new double[termNum];
			
			countTf(tokens, termIndex[docIdx], tf[docIdx]);
			
			for(int i=0; i<termIndex[docIdx].length; i++) {
				int term = termIndex[docIdx][i];
				df[term]++;
			}
		}
		calculateIdf();
		calculateTfidf();
		
	}
	
	protected void calculateTfidf() {
		for(int doc=0; doc < numOfDocs; doc++) {
			for(int t=0; t<termIndex[doc].length; t++) {
				int term = termIndex[doc][t];
				tfidf[doc][t] = tf[doc][t] * idf[term];
			}
		}
	}

	protected void calculateIdf() {
		for(int i=0; i<df.length; i++) {
			idf[i] = Math.log((double)numOfDocs / (double)df[i]);
		}
	}
	
	/*
	 * Check how many distinct numbers in an array.
	 */
	public int distinctNumbers(int[] numbers) {
		if(numbers.length == 0) return 0;
		Arrays.sort(numbers);
		int total = 1;
		int value = numbers[0];
		for(int i=1; i<numbers.length; i++) {
			if(numbers[i] != value) {
				total++;
				value = numbers[i];
			}
		}
		return total;
	}
	
	
	/**
	 * Count term frequency for a sequence of tokens.
	 * @param tokens
	 * @param terms
	 * @param freq
	 */
	protected void countTf(int[] tokens, int[] terms, int[] freq) {
		Arrays.sort(tokens);
		Arrays.fill(freq, 0);
		int si = 0, ti = 0;
		terms[ti] = tokens[si];
		freq[0] = 1;
		for (; si<tokens.length; si++) {
			if (terms[ti] != tokens[si]) {
				ti++;
				terms[ti] = tokens[si];
			}
			freq[ti]++;
		}
	}

	private double getTfidfSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		double[] qTfidf = tfidf[qdocId];
		double[] tTfidf = tfidf[targetDocId];
		
		int[] qTerm = termIndex[qdocId];
		int[] tTerm = termIndex[targetDocId];
		
		int qi = 0;
		int ti = 0;
		
		double sim = 0;
		
		while(qi<qTerm.length && ti<tTerm.length) {
			if(qTerm[qi] == tTerm[ti]) {
				sim += qTfidf[qi] * tTfidf[ti];
				qi++;
				ti++;
			} else {
				if (qTerm[qi] < tTerm[ti]) 
					qi++;
				else 
					ti++;
			}
		}
		
		return sim/(normalizeVector(qTfidf) * normalizeVector(tTfidf));
	}
	
	public double cosineProduct(double[] v1, double[] v2) {
		double sim = 0;
		for(int i=0; i<v1.length; i++) {
			sim += v1[i] * v2[i];
		}
		
		return sim/(normalizeVector(v1) * normalizeVector(v2));
	}
	
	public double normalizeVector(double[] v) {
		double sum = 0;
		for(int i=0; i<v.length; i++) {
			sum += v[i] * v[i];
		}
		return Math.sqrt(sum);
	}
	
	public void retrieveTask1Solution(String queryFile, String solutionFile) {
		if(solver == null) initSolver();
		try {
			solver.retrieveTask1Solution(queryFile, solutionFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException {
//		String malletFile = "dataset/vlc_lectures.all.en.f8.filtered.mallet";
		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
//		String malletFile = "dataset/vlc_lectures.all.5000term.mallet";
		String queryFile = "dataset/task1_query.en.f8.txt";
		String targetFile = "dataset/task1_target.en.f8.txt";
		String solutionFile = "dataset/task1_solution.en.f8.tfidf.txt";
		InstanceList documents = InstanceList.load(new File(malletFile));
		FeatureSequence fs = (FeatureSequence)documents.get(0).getData();
		String s = (String)fs.getObjectAtPosition(0);
		
		MalletTfidf mt = new MalletTfidf(documents);
		mt.retrieveTask1Solution(queryFile, solutionFile);
		try {
			double precision = Task1Solution.evaluateResult(targetFile, solutionFile);
			System.out.println("Tfidf precision: " + precision);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
