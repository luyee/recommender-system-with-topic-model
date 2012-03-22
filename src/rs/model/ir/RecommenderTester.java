package rs.model.ir;

import java.io.File;
import java.io.IOException;

import rs.model.*;
import rs.model.ir.combination.*;
import rs.model.ir.separate.*;
import rs.util.vlc.solution.*;
import rs.model.recommender.*;
import rs.model.topics.NetPLSA;
import rs.model.topics.PrimeRtm;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;

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
	
	private void setModel(Model model) {
		this.model = model;
		this.solver = new Task1Solution(model);
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
		while (lambda < 0.3) {
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
		setModel(clusterMle);
//		for(alpha = 0.1; alpha<1; alpha += 0.2) {
//			for(beta = 0.1; beta<1; beta += 0.2) {
				for (lambda = 0.1; lambda<1; lambda += 0.05) {
			clusterMle.setSmoothParameters(alpha, beta, lambda);
			runTest("ClusterMle", "Lambda: " + lambda);
				}
//			}
//		}
	}
	
	public void tfidfMleCombineTest() {
		double lambda = 0.01;
		double mleLambda = 0.11;
		TfidfMleCombine tModel = new TfidfMleCombine(documents);
		model = (Model)tModel;
		solver = new Task1Solution(model);
		for(; lambda < 1; lambda += 0.05) {
			tModel.setParam(lambda, mleLambda);
			runTest("TfidfMleCombine", "Lambda: " + lambda + ", MleLambda: " + mleLambda);
		}
	}
	public void parallelLdaTest() throws IOException {
		int numTopics = 160;
		int numIterations = 2000;
		double alpha = 0.0016;
		double beta = 0.0001;
		
		InstanceList documents = InstanceList.load(new File(malletFile));
		ParallelTopicModel lda = new ParallelTopicModel(numTopics, alpha*numTopics, beta);
		lda.addInstances(documents);
		lda.setNumThreads(4);
		lda.setNumIterations(numIterations);
		lda.setSymmetricAlpha(false);
		lda.setTopicDisplay(1000, 5);
		lda.printLogLikelihood = false;
		lda.estimate();
		lda.printTopWords(System.out, 10, true);
		
		ParallelLdaRecommender pModel = new ParallelLdaRecommender(documents);
		pModel.setPtm(lda);
		pModel.calculateProb();
		setModel(pModel);
		runTest("ParallelLDaRecommender", "alpha: " + alpha + ", beta: " + beta + 
					", #topics: " + numTopics + ", #iterations: " + numIterations);
		
	}
	
	public void ldaTfidfCombineTest() throws IOException {
		MalletTfidf tfidfModel = new MalletTfidf(documents);
		ParallelLdaRecommender ldaModel = new ParallelLdaRecommender(documents);
		int numIterations = 2000;
		double alpha = 0.0016;
		int numTopics = 160;
		double beta = 0.0048;
		
		double alphaSum = alpha * numTopics;
		ParallelTopicModel lda = new ParallelTopicModel(numTopics, alphaSum, beta);
		lda.addInstances(documents);
		lda.setNumThreads(4);
		lda.setNumIterations(numIterations);
		lda.setTopicDisplay(1000, 5);
		lda.printLogLikelihood = false;
		lda.estimate();
		System.out.println("LDA parameter, alphaSum: " + lda.alphaSum + ", beta: " + lda.beta);
		
		ldaModel.setPtm(lda);
		ldaModel.calculateProb();
		
		LdaTfidfCombination pModel = new LdaTfidfCombination(tfidfModel, ldaModel);
		setModel(pModel);
		for(double lambda=0.01; lambda < 1; lambda+=0.05) {
			pModel.setLambda(lambda);
			runTest("ldaTfidfCombineTest", "LDA parameter, alphaSum: " + lda.alphaSum + ", beta: " + lda.beta +
					", lambda: " + lambda);
		}
	}

	
	
	public void netPlsaTest() {
		double lambda = 0.01;
		double gamma = 0.01;
		int numOfTopics = 160;
		NetPLSA netPlsa = new NetPLSA(numOfTopics, lambda, gamma, trainMalletFile, linkFile);
		netPlsa.train(30);
//		netPlsa.printTopWords(10, true);
		
		InstanceList testDocuments = InstanceList.load(new File(testMalletFile));
		InstanceList allDocuments = InstanceList.load(new File(malletFile));
		
		netPlsa.test(testDocuments, 100);
		
		NetPLsaRecommender nModel = new NetPLsaRecommender(allDocuments, netPlsa);
		setModel(nModel);
		runTest("netPlsa", "gamma: " + gamma);
	}
	
	public void rtmTest() throws IOException {
		int numOfTopic = 80;
		int numIter = 50;
		double alpha = 0.0016;
		double beta = 0.0048;
		PrimeRtm rtm = new PrimeRtm(numOfTopic, alpha*numOfTopic, beta);
		rtm.initFromFile(malletFile, linkFile);

		Randoms r = new Randoms();
		rtm.estimate(numIter, r, 20);
		rtm.printTopWords (10, true);		
		RtmRecommender rModel = new RtmRecommender(rtm);
		rModel.calculateProb();
		setModel(rModel);
		
		runTest("RTM", "#topics: " + numOfTopic);
		
		MalletTfidf tfidfModel = new MalletTfidf(documents);
		RtmTfidfCombinaton rtmTfidf = new RtmTfidfCombinaton(tfidfModel, rModel);
		setModel(rtmTfidf);
		for(double lambda = 0.01; lambda<1; lambda += 0.05) {
			runTest("RtmTfidfCombination", "Lambda: " + lambda);
		}
	}
	
	public static void main(String[] args) {
		for(int i=0; i<4; i++) {
		String malletFile = "dataset/vlc/folds/all." + i+ ".4189.mallet";
		String trainMalletFile = "dataset/vlc/folds/training." + i + ".mallet";
		String testMalletFile = "dataset/vlc/folds/test." + i + ".mallet";
		String queryFile = "dataset/vlc/folds/query." + i + ".csv";
		String linkFile = "dataset/vlc/folds/trainingPairs." + i + ".csv";
		String targetFile = "dataset/vlc/folds/target." + i + ".csv";
		String solutionFile = "dataset/vlc/task1_solution.txt";
		
		RecommenderTester tester = new RecommenderTester(malletFile, trainMalletFile, 
				testMalletFile, linkFile, queryFile, targetFile, solutionFile);
		tester.tfidfTest();
//		tester.tfidfMleCombineTest();
//		tester.separateTfidfTest();
//		tester.mleTest();
//		tester.separateMleTest();
		tester.clusterMleTest();
//		try {
//			tester.parallelLdaTest();
//			tester.rtmTest();
//
//			tester.ldaTfidfCombineTest();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		tester.netPlsaTest();
	}
	}
}
