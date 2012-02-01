package rs.topics.separate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;

import rs.topics.model.RelationalTopicModel;
import rs.types.PairedInfo;
import rs.util.vlc.Task1Solution;
import cc.mallet.types.*;

public class SeparatePLdaRegression extends SeparateParallelLda {
	public final static String BY = RelationalTopicModel.BY; 
	
	public TObjectIntHashMap<String> idHash;
	public TObjectIntHashMap<String> pairIdHash; 	// v1xv2 -> int
	public ArrayList <String> pairIds;
	public ArrayList<PairedInfo> links;
	public int numOfLinks;
	
	double[] y;
	double[][] x;
	double[] eta;

	public SeparatePLdaRegression(InstanceList docs, int testIndex) {
		super(docs, testIndex);
	}
	
	public void initSolver(InstanceList documents) {
		solver = new Task1Solution(documents) {
			public String recommend(String  qId) {
				int qdocId = idHash.get(qId);
				double[] predSim = regressionSimilarity(qdocId);
				String line = sortRecommendList(qdocId, predSim);
				return line;
			}
		};
	}
	
	public void initFromRelationalTopicModel(String malletFile, String pairFile) throws IOException {
		RelationalTopicModel rtm = new RelationalTopicModel(20);
		rtm.initFromFile(malletFile, pairFile);
		this.idHash = rtm.idHash;
		this.pairIdHash = rtm.pairIdHash;
		this.pairIds = rtm.pairIds;
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
	
	protected double[] regressionSimilarity(int qdocId) {
		// TODO Auto-generated method stub
		if(eta == null)	params();
		double[] predSim = new double[test.size()];
		Arrays.fill(predSim, 0);
		
		for(int ti=0; ti<test.size(); ti++) {
			double[] v1 = lda.getTopicProbabilities(qdocId);
			double[] v2 = testTopicDistribution[ti];
			predSim[ti] = rs.util.vlc.Util.weightedProduct(v1, v2, eta);
		}
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
		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
		String simFile = "dataset/vlc/sim5p.csv";
		String solutionFile = "dataset/vlc/task1_solution.en.f8.lm.txt";
		String queryFile = "dataset/task1_query.en.f8.txt";
		String targetFile = "dataset/task1_target.en.f8.txt";
		
		int numTopics = 160;
		int numIterations = 500;
		double alpha = 0.0016;
		double beta = 0.0001;
		
		InstanceList documents = InstanceList.load(new File(malletFile));
		SeparatePLdaRegression spl = new SeparatePLdaRegression(documents, Task1Solution.testIndexStart);
		try {
			spl.initFromRelationalTopicModel(malletFile, simFile);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		spl.trainDocuments(numTopics, numIterations, alpha, beta);
		spl.generateTestInference();
//		spl.lda.printTopWords(System.out, 10, true);
		spl.params();
		spl.retrieveTask1Solution(queryFile, solutionFile);
		
		double precision;
		try {
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
