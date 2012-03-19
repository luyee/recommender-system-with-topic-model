package rs.model.ir;

import java.io.File;
import java.io.IOException;

import rs.model.*;
import rs.model.ir.separate.*;
import rs.util.vlc.solution.*;

import cc.mallet.types.InstanceList;

public class RecommenderTester {
	String malletFile;
	String trainMalletFile;
	String testMalletFile;
	String linkFile;
	String queryFile;
	String targetFile;
	String solutionFile;
	
	InstanceList documents;
	InstanceList trainDoc;
	InstanceList testDoc;
	
	Model model;
	BasicTask1Solution solver;
	
	double precision;
	
	public RecommenderTester(String malletFile, String train, String test, 
								String link, String query, String target, String solution) {
		this.malletFile = malletFile;
		this.trainMalletFile = train;
		this.testMalletFile = test;
		this.linkFile = link;
		this.queryFile = query;
		this.targetFile = target;
		this.solutionFile = solution;
		
		this.documents = InstanceList.load(new File(malletFile));
		this.trainDoc = InstanceList.load(new File(trainMalletFile));
		this.testDoc = InstanceList.load(new File(testMalletFile));
	}

	public void printPrecision(String mStr, String param) {
		System.out.println("Model: " + mStr + "," + param + ", precision: " + precision );
	}
	
	private void runTest(String mStr, String param) {
		try {
			solver.retrieveTask1Solution(queryFile, solutionFile);
			precision = BasicTask1Solution.evaluateResult(targetFile, solutionFile);
			printPrecision(mStr, param);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public void tfidfTest() {
		model = new MalletTfidf(documents);
		solver = new Task1Solution(model);
		runTest("Tfidf", "overall");
	}
	
	public void separateTfidfTest() {
		model = new SeparateMalletTfidf(trainDoc, testDoc);
		solver = new Task1SolutionWithSeparateData((SeparateModel)model);
		runTest("SpearateTfidf", "spearate");
	}
	
	public void mleTest() {
		MalletMle mModel = new MalletMle(documents);
		model = (Model)mModel;
		solver = new Task1Solution(model);
		double lambda = 0.01;
		double miu = 1000;
		while (lambda < 1) {
			mModel.setLambda(lambda);
			runTest("Mle", "Lambda: " + lambda);
			lambda += 0.05;
		}
	}
	
	public void separateMleTest() {
		SeparateMalletMle sModel = new SeparateMalletMle(trainDoc, testDoc);
		model = (Model)sModel;
		solver = new Task1SolutionWithSeparateData(sModel);
		double lambda = 0.01;
		while(lambda < 1) {
			sModel.setLambda(lambda);
			runTest("SeparateMle", "Lambda: " + lambda);
			lambda += 0.05;
		}
	}
	
	public void clusterMleTest() {
		double alpha = 0.01, beta = 0.01, lambda = 0.01;
		
		MalletMleCluster clusterMle = new MalletMleCluster(malletFile, linkFile, alpha);
		model = (Model)clusterMle;
		solver = new Task1Solution(model);
//		for(alpha = 0.1; alpha<1; alpha += 0.2) {
//			for(beta = 0.1; beta<1; beta += 0.2) {
				for (lambda = 0.1; lambda<1; lambda += 0.05) {
			clusterMle.setSmoothParameters(alpha, beta, lambda);
			runTest("ClusterMle", "Lambda: " + lambda);
				}
//			}
//		}
	}
	
	public static void main(String[] args) {
		String malletFile = "dataset/vlc/folds/all.0.4189.mallet";
		String trainMalletFile = "dataset/vlc/folds/training.0.mallet";
		String testMalletFile = "dataset/vlc/folds/test.0.mallet";
		String queryFile = "dataset/vlc/folds/query.0.csv";
		String linkFile = "dataset/vlc/folds/trainingPairs.0.csv";
		String targetFile = "dataset/vlc/folds/target.0.csv";
		String solutionFile = "dataset/vlc/task1_solution.en.mle.txt";
		
		RecommenderTester tester = new RecommenderTester(malletFile, trainMalletFile, 
				testMalletFile, linkFile, queryFile, targetFile, solutionFile);
//		tester.tfidfTest();
//		tester.separateTfidfTest();
//		tester.mleTest();
		tester.separateMleTest();
//		tester.clusterMleTest();
	}
}
