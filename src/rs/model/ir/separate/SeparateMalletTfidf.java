/**
 * This class tries to get tf-idf features based on mallet model. 
 * Because mallet already has the feature sequence, we can easily 
 * make use of that list. Furthermore, I think it may be necessary 
 * to do some further pre-processing so that punctuations and too 
 * short words should be eliminated.
 */
package rs.model.ir.separate;

import java.io.*;
import java.util.Arrays;

import cc.mallet.types.*;
import rs.model.SeparateModel;
import rs.model.ir.MalletTfidf;
import rs.util.vlc.*;
import rs.util.vlc.solution.BasicTask1Solution;
import rs.util.vlc.solution.Task1Solution;
import rs.util.vlc.solution.Task1SolutionWithSeparateData;

public class SeparateMalletTfidf extends SeparateModel {
	public MalletTfidf training;
	public MalletTfidf test;

	public SeparateMalletTfidf(InstanceList train, InstanceList test) {
		this.training = new MalletTfidf(train);
		this.test = new MalletTfidf(test);
	}
	
	public InstanceList getTrainingDocuments() {
		return this.training.getTrainingDocuments();
	}
	
	public InstanceList getTestDocuments() {
		return this.test.getTrainingDocuments();
	}
	
	public double getSim(int qdocId, int targetDocId) {
		return getTfidfSim(qdocId, targetDocId);
	}

	public double getTfidfSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		double[] qTfidf = training.tfidf[qdocId];
		int[] tTf = test.tf[targetDocId];
		
		
		int[] qTerm = training.termIndex[qdocId];
		int[] tTerm = test.termIndex[targetDocId];
		
//		double[] tTfidf = new double[tTf.length];
//		for(int i=0; i<tTfidf.length; i++) {
//			int term = tTerm[i];
//			tTfidf[i] = tTf[i] * training.idf[term];
//		}
		double[] tTfidf = test.tfidf[targetDocId];
		
		int qi = 0;
		int ti = 0;
		
		double sim = 1;
		
		while(qi<qTerm.length && ti<tTerm.length) {
			if(qTerm[qi] == tTerm[ti]) {
				int term = qTerm[qi];
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
		
		return sim/(Util.normalizeVector(qTfidf) * Util.normalizeVector(tTfidf));
	}

	public static void main(String[] args) throws IOException {
		String trainMallet = "dataset/vlc/folds/training.0.mallet";
		String testMallet = "dataset/vlc/folds/test.0.mallet";

		String queryFile = "dataset/vlc/folds/query.0.csv";
		String targetFile = "dataset/vlc/folds/target.0.csv";

		String solutionFile = "dataset/vlc/task1_solution.en.f8.septfidf.txt";
		InstanceList train = InstanceList.load(new File(trainMallet));
		InstanceList test = InstanceList.load(new File(testMallet));
		
		SeparateMalletTfidf mt = new SeparateMalletTfidf(train, test);
		BasicTask1Solution solver = new Task1SolutionWithSeparateData(mt);
		
		try {
			solver.retrieveTask1Solution(queryFile, solutionFile);
			double precision = Task1Solution.evaluateResult(targetFile, solutionFile);
			System.out.println("Separate tfidf precision: " + precision);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
