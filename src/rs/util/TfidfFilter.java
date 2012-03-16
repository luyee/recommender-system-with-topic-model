/**
 * Here tf-idf is a global weight, meaning term frequency is also a global tf.
 */

package rs.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import rs.text.ir.MalletTfidf;
import rs.text.topics.RelationalTopicModel;
import rs.util.vlc.Task1Solution;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import gnu.trove.map.hash.*;

public class TfidfFilter {
	
	class TermRank implements Comparable {
		double tfidf;
		int termIdx;
		public TermRank (double tfidf, int termIdx) { this.tfidf = tfidf; this.termIdx = termIdx; }
		/**
		 * Sort in reverse order
		 * @param o2
		 * @return
		 */
		public final int compareTo (Object o2) {
			if (tfidf > ((TermRank)o2).tfidf)
				return -1;
			else if (tfidf == ((TermRank)o2).tfidf)
				return 0;
			else return 1;
		}
	}
	
	public final int FILTERTHRESHOLD = 2000;
	
	public int[][] termIndex;		// indexed by <document, term>, this indicates which terms appear in each doc
	public int[] tf;				// indexed by <document, term>, indicating frequency of each term
	public int[] df;				// document frequency for each term
	public double[] idf;			// inverse document frequency for each term
	public double[] tfidf;		// tfidf for each term in document, indexed by <document, term>
	public int[] docLength;
	public int numOfTerms;
	public int numOfDocs;
	
	public static final int testIndexStart = RelationalTopicModel.testIndexStart;

	public InstanceList documents;
		
	public TfidfFilter(InstanceList doc) {
		this.documents = doc;		
		initTfidf();
	}
	
	
	public void initTfidf() {
		numOfDocs = documents.size();
		numOfTerms = documents.getDataAlphabet().size();
		idf = new double[numOfTerms];
		df = new int[numOfTerms];
		termIndex = new int[documents.size()][];
		tf = new int[numOfTerms];
		tfidf = new double[numOfTerms];
		docLength = new int[documents.size()];
		
		Arrays.fill(df, 0);
		
		for(int docIdx=0; docIdx < documents.size(); docIdx++) {
			FeatureSequence fs = (FeatureSequence)documents.get(docIdx).getData();
			docLength[docIdx] = fs.getLength();
			int[] tokens = fs.toSortedFeatureIndexSequence();
			int termNum = distinctNumbers(tokens);
			termIndex[docIdx] = new int[termNum];
			
			countTfForOneDoc(tokens, termIndex[docIdx], tf);
			
			for(int i=0; i<termIndex[docIdx].length; i++) {
				int term = termIndex[docIdx][i];
				df[term]++;
			}
		}
		calculateIdf();
		calculateTfidf();
	}
	
	protected void calculateTfidf() {
		for(int i=0; i<df.length; i++) {
			tfidf[i] = Math.log((double)tf[i]) * idf[i];
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
	protected void countTfForOneDoc(int[] tokens, int[] terms, int[] tf) {
		Arrays.sort(tokens);
		int si = 0, ti = 0;
		terms[ti] = tokens[si];
		for (; si<tokens.length; si++) {
			int term = tokens[si];
			if (terms[ti] != tokens[si]) {
				ti++;
				terms[ti] = tokens[si];
			}
			tf[term]++;
		}
	}
	
	protected TermRank[] sortTerms() {
		TermRank[] rankedTerms = new TermRank[numOfTerms];
		for(int i=0; i<numOfTerms; i++) {
			rankedTerms[i] = new TermRank(tfidf[i], i);
		}
		Arrays.sort(rankedTerms);
		return rankedTerms;
	}
	
	public void filterTerms(String outputFile) {
		TermRank[] rankedTerms = sortTerms();
		Alphabet vocab = documents.getDataAlphabet();
		
		TIntIntHashMap neededTerms = new TIntIntHashMap();
		for(int i=0; i<FILTERTHRESHOLD; i++) {
			neededTerms.put(rankedTerms[i].termIdx, 1);
		}
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			
			for(int i=0; i<documents.size(); i++) {
				StringBuilder sb = new StringBuilder();
				sb.append((String)documents.get(i).getName());
				sb.append(" en ");
				FeatureSequence fs = (FeatureSequence)documents.get(i).getData();
				for(int t=0; t<fs.getLength(); t++) {
					int token = fs.getIndexAtPosition(t);
					if(neededTerms.containsKey(token)) {
						sb.append((String)vocab.lookupObject(token) + " ");
					}
				}
				String temps = sb.toString();
				if(temps.split(" ").length > 2) {
					writer.write(sb.toString());
					writer.newLine();
				}
			}		
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public static void main(String[] args) throws IOException {
//		String malletFile = "dataset/vlc_lectures.all.en.f8.filtered.mallet";
		String malletFile = "dataset/vlc/vlc_lectures.all.en.f8.mallet";
		String targetFile = "dataset/vlc_lectures.all.2000term.txt";
		InstanceList documents = InstanceList.load(new File(malletFile));
		TfidfFilter filter = new TfidfFilter(documents);
		filter.filterTerms(targetFile);
	}
}
