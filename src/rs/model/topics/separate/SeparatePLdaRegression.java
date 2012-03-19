package rs.model.topics.separate;

import java.io.*;
import java.util.ArrayList;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;

import rs.model.topics.RelationalTopicModel;
import rs.types.PairedInfo;
import rs.util.vlc.solution.*;
import cc.mallet.types.*;

public class SeparatePLdaRegression extends SeparateParallelLda {
	public final static String BY = RelationalTopicModel.BY; 
	
	public TObjectIntHashMap<String> idHash;
	public TObjectIntHashMap<String> pairIdHash; 	// v1xv2 -> int
	public ArrayList<PairedInfo> links;
	public int numOfLinks;
	
	double[] y;
	double[][] x;
	double[] eta;

	public SeparatePLdaRegression(InstanceList train, InstanceList test) {
		super(train, test);
	}
	
	public double getSim(int qdocId, int targetDocId) {
		return regressionSimilarity(qdocId, targetDocId);
	}
	
	public void initFromRelationalTopicModel(String malletFile, String pairFile) throws IOException {
		RelationalTopicModel rtm = new RelationalTopicModel(20);
		rtm.initFromFile(malletFile, pairFile);
		this.idHash = rtm.idHash;
		this.pairIdHash = rtm.pairIdHash;
		this.links = rtm.links;
		this.numOfLinks = rtm.numOfLinks;
		this.y = rtm.y;
	}
	
	public void params() {
		if(lda == null) {
			System.out.println("Should run lda estimation first.");
			System.exit(1);
			return;
		}
		if (testTopicDistribution == null) 
			generateTestInference();
		if(x==null)
			_calculateXValue();
		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
		regression.newSampleData(y, x);
		this.eta = regression.estimateRegressionParameters();
	}
	
	protected double regressionSimilarity(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		if(eta == null)	params();
		double predSim = 0;
		
		double[] v1 = lda.getTopicProbabilities(qdocId);
		double[] v2 = testTopicDistribution[targetDocId];
		predSim = rs.util.vlc.Util.weightedProduct(v1, v2, eta);
		
		return predSim;
	}
	
	private void _calculateXValue() {
		if(lda == null) {
			System.out.println("Should run lda estimation first.");
			System.exit(1);
			return;
		}
		if (testTopicDistribution == null) 
			generateTestInference();
		if (x == null) {
			x = new double[numOfLinks][lda.numTopics];
		}

		TObjectIntIterator<String> iterator = pairIdHash.iterator();
		while(iterator.hasNext()) {
			iterator.advance();
			String key = iterator.key();
			String[] ids = key.split(BY);
			int v1 = Integer.parseInt(ids[0]);
			int v2 = Integer.parseInt(ids[1]);
			int docIdx = iterator.value();
			double[] d1 = lda.getTopicProbabilities(v1);
			double[] d2 = lda.getTopicProbabilities(v2);
			for(int i=0; i<x[docIdx].length; i++) {
				x[docIdx][i] = d1[i] * d2[i]; 
			}
		}
	}
	
	public static void main(String[] args) {
//		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
//		String simFile = "dataset/vlc/sim5p.csv";
//		String solutionFile = "dataset/vlc/task1_solution.en.f8.lm.txt";
//		String queryFile = "dataset/task1_query.en.f8.txt";
//		String targetFile = "dataset/task1_target.en.f8.txt";
		
		String malletFile = "dataset/vlc/folds/all.0.4189.mallet";
		String trainMalletFile = "dataset/vlc/folds/training.0.mallet";
		String testMalletFile = "dataset/vlc/folds/test.0.mallet";
		String queryFile = "dataset/vlc/folds/query.0.csv";
		String linkFile = "dataset/vlc/folds/trainingPairs.0.csv";
		String targetFile = "dataset/vlc/folds/target.0.csv";
		String solutionFile = "dataset/vlc/task1_solution.en.f8.lm.txt";
		
		int numTopics = 160;
		int numIterations = 200;
		double alpha = 0.0048;
		double beta = 0.0001;
		
		InstanceList train = InstanceList.load(new File(trainMalletFile));
		InstanceList test = InstanceList.load(new File(testMalletFile));
		SeparatePLdaRegression spl = new SeparatePLdaRegression(train, test);
		try {
			spl.initFromRelationalTopicModel(malletFile, linkFile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		spl.trainDocuments(numTopics, numIterations, alpha, beta);
		spl.generateTestInference();
//		spl.lda.printTopWords(System.out, 10, true);
		spl.params();
		BasicTask1Solution solver = new Task1SolutionWithSeparateData(spl);
		double precision;
		try {
			solver.retrieveTask1Solution(queryFile, solutionFile);
			precision = Task1Solution.evaluateResult(targetFile, solutionFile);	
			System.out.println(String.format(
					"SeparateParallelLda: iteration: %d, precisoion: %f", 
					numIterations, precision));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
