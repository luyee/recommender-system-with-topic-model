package rs.model.topics.separate;

import java.io.*;
import java.util.*;

import rs.model.*;
import rs.util.vlc.solution.*;

import cc.mallet.topics.*;
import cc.mallet.types.*;

public class SeparateParallelLda extends SeparateModel {
	InstanceList training;
	InstanceList test;
	
	public double[][] testTopicDistribution;	
	public ParallelTopicModel lda;
	
	public SeparateParallelLda(InstanceList train, InstanceList test) {
		this.training = train;
		this.test = test;
	}
	

	@Override
	public InstanceList getTestDocuments() {
		// TODO Auto-generated method stub
		return this.test;
	}

	@Override
	public double getSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public InstanceList getTrainingDocuments() {
		// TODO Auto-generated method stub
		return this.training;
	}

	protected double queryVsmSimilarity(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		double predSim = 0;
		
		double[] v1 = lda.getTopicProbabilities(qdocId);
		double[] v2 = testTopicDistribution[targetDocId];
		predSim = rs.util.vlc.Util.cosineProduct(v1, v2);

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
		double alpha = 0.0016;
		double beta = 0.0001;
		
		InstanceList train = InstanceList.load(new File(trainMalletFile));
		InstanceList test = InstanceList.load(new File(testMalletFile));
		SeparateParallelLda spl = new SeparateParallelLda(train, test);
		spl.trainDocuments(numTopics, numIterations, alpha, beta);
		spl.generateTestInference();
		spl.lda.printTopWords(System.out, 10, true);
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
