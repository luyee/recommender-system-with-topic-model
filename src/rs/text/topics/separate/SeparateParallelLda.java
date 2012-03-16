package rs.text.topics.separate;

import java.io.*;
import java.util.*;

import rs.util.vlc.Task1Solution;

import cc.mallet.topics.*;
import cc.mallet.types.*;

public class SeparateParallelLda {
	InstanceList complete;
	InstanceList training;
	InstanceList test;
	
	public double[][] testTopicDistribution;	
	public ParallelTopicModel lda;
	public Task1Solution solver;
	
	public SeparateParallelLda(InstanceList docs, int testIndex) {
		this.complete = docs;
		divideDocuments(docs, testIndex);
		initSolver(docs);
	}
	
	protected void initSolver(InstanceList documents) {
		// TODO Auto-generated method stub
		solver = new Task1Solution(documents) {
			public String recommend(String  qId) {
				int qdocId = idHash.get(qId);
				double[] predSim = queryVsmSimilarity(qdocId);
				String line = sortRecommendList(qdocId, predSim);
				return line;
			}
		};
	}

	protected double[] queryVsmSimilarity(int qdocId) {
		// TODO Auto-generated method stub
		double[] predSim = new double[test.size()];
		Arrays.fill(predSim, 0);
		
		for(int ti=0; ti<test.size(); ti++) {
			double[] v1 = lda.getTopicProbabilities(qdocId);
			double[] v2 = testTopicDistribution[ti];
			predSim[ti] = rs.util.vlc.Util.cosineProduct(v1, v2);
		}
		return predSim;
	}

	/**
	 * Initialize this separate model using a complete list.
	 * @param documents
	 * @param testStartIndex
	 */
	public void divideDocuments(InstanceList documents, int testStartIndex) {
		Alphabet dataAlpha = documents.getDataAlphabet();
		Alphabet targetAlpha = documents.getTargetAlphabet();
		
		this.training = new InstanceList(dataAlpha, targetAlpha);
		this.test = new InstanceList(dataAlpha, targetAlpha);
		int di = 0;
		for(di=0; di<testStartIndex; di++) {
			training.add(documents.get(di));
		}
		for(di=testStartIndex; di<documents.size(); di++) {
			test.add(documents.get(di));
		}
	}
	
	public void trainDocuments(InstanceList documents, int numTopics, int numIterations, double alpha, double beta) {
		double alphaSum = alpha * numTopics;
		lda = new ParallelTopicModel(numTopics, alphaSum, beta);
		lda.addInstances(documents);
		lda.setNumThreads(4);
		lda.setNumIterations(numIterations);
		lda.printLogLikelihood = false;
		try {
			lda.estimate();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("LDA parameter, alphaSum: " + lda.alphaSum + ", beta: " + lda.beta);
	}
	
	public void generateTestInference() {
		if(lda == null) {
			System.out.println("Should run lda estimation first.");
			System.exit(1);
			return;
		}
		if (testTopicDistribution == null) 
			testTopicDistribution = new double[test.size()][];
		TopicInferencer infer = lda.getInferencer();
		int iterations = 800;
		int thinning = 5;
		int burnIn = 100;
		for(int ti=0; ti<test.size(); ti++) {
			testTopicDistribution[ti] = infer.getSampledDistribution(test.get(ti), iterations, thinning, burnIn);
		}
	}
		
	public void trainDocuments(int numTopics, int numIterations, double alpha, double beta) {
		this.trainDocuments(this.training, numTopics, numIterations, alpha, beta);
	}
	
	public InstanceList getTraining() {
		return training;
	}

	public void setTraining(InstanceList training) {
		this.training = training;
	}

	public InstanceList getTest() {
		return test;
	}

	public void setTest(InstanceList test) {
		this.test = test;
	}

	public ParallelTopicModel getLda() {
		return lda;
	}

	public void setLda(ParallelTopicModel lda) {
		this.lda = lda;
	}
	
	public void retrieveTask1Solution(String queryFile, String solutionFile) {
		if (lda == null) return;
		if (testTopicDistribution == null)
			generateTestInference();
		try {
			solver.retrieveTask1Solution(queryFile, solutionFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
		String simFile = "dataset/vlc/sim5p.csv";
		String solutionFile = "dataset/vlc/task1_solution.en.f8.lm.txt";
		String queryFile = "dataset/task1_query.en.f8.txt";
		String targetFile = "dataset/task1_target.en.f8.txt";
		
		int numTopics = 160;
		int numIterations = 2000;
		double alpha = 0.0016;
		double beta = 0.0001;
		
		InstanceList documents = InstanceList.load(new File(malletFile));
		SeparateParallelLda spl = new SeparateParallelLda(documents, Task1Solution.testIndexStart);
		spl.trainDocuments(numTopics, numIterations, alpha, beta);
		spl.generateTestInference();
		spl.lda.printTopWords(System.out, 10, true);
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
