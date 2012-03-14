package rs.text.recommender;

import gnu.trove.iterator.TObjectIntIterator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import rs.text.model.RelationalTopicModel;
import rs.util.vlc.Task1Solution;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

public class LdaIdfTfidfRegressionCombination {
	LDAIdfExplorer ldaIdfModel;
	MalletTfidf tfidfModel;	
	RelationalTopicModel rtm;
	double lambda;	
	public Task1Solution solver;
	public static final int testIndexStart = RelationalTopicModel.testIndexStart;
	
	public double[] y;					// value of normalized pair feature
	public double[][] x;				// value for regression
	public double[] eta;
	
	public LdaIdfTfidfRegressionCombination (MalletTfidf tfidf, LDAIdfExplorer ldaidf) {
		this.ldaIdfModel = ldaidf;
		this.tfidfModel = tfidf;
	}
	
	public void initSolver() {
		solver = new Task1Solution(tfidfModel.documents) {
			public String recommend(String qId) {
				int qdocId = idHash.get(qId);
				
				int test_size = documents.size() - testIndexStart;
				double[] predSim = new double[test_size];
				Arrays.fill(predSim, 0);
				for(int i=0; i<test_size; i++) {
//					predSim[i] = lambda * tfidfModel.getTfidfSim(qdocId, testIndexStart+i) 
//								+ (1 - lambda) * 0.2 * ldaIdfModel.tfidfRecommender.getTfidfSim(qdocId, testIndexStart + i);
					predSim[i] = tfidfModel.getTfidfSim(qdocId, testIndexStart+i) 
										* ldaIdfModel.tfidfRecommender.getTfidfSim(qdocId, testIndexStart + i);
				}
				String line = sortRecommendList(qdocId, predSim);
				return line;
			}
		};
	}
	
	public void retrieveTask1Solution(String queryFile, String solutionFile, double lambda) {
		this.lambda = lambda;
		initSolver();
		try {
			solver.retrieveTask1Solution(queryFile, solutionFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException {
//		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
//		String queryFile = "dataset/task1_query.en.f8.txt";
//		String targetFile = "dataset/task1_target.en.f8.txt";
		String solutionFile = "dataset/task1_solution.en.f8.combine.txt";
//		String simFile = "dataset/vlc/sim5p.csv";
		
		String malletFile = "dataset/vlc/vlc_lectures.all.en.f8.mallet";
		String simFile = "dataset/vlc/sim_0p_100n.csv";
//		String queryFile = "dataset/task1_query.en.f8.txt";
//		String targetFile = "dataset/task1_target.en.f8.txt";
		
		String queryFile = "dataset/vlc/task1_query.en.f8.n5.txt";
		String targetFile = "dataset/vlc/task1_target.en.f8.n5.txt";
		
		InstanceList documents = InstanceList.load(new File(malletFile));
		MalletTfidf tfidfModel = new MalletTfidf(documents);
//		mt.retrieveTask1Solution(queryFile, solutionFile);
		
		LDAIdfExplorer ldaIdfModel = new LDAIdfExplorer(documents);
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
		
		ldaIdfModel.setPtm(lda);
		ldaIdfModel.calculateProb();
		ldaIdfModel.calculateTopicIdf();
		
		LdaIdfTfidfRegressionCombination model = new LdaIdfTfidfRegressionCombination(tfidfModel, ldaIdfModel);
		
		RelationalTopicModel rtm = new RelationalTopicModel(numTopics, alpha*numTopics, beta);
		rtm.initFromFile(malletFile, simFile);
		
		model.setRtm(rtm);
		
		double lambda = 0.1;
		for(; lambda<1; lambda = lambda*1.1) {
			model.retrieveTask1Solution(queryFile, solutionFile, lambda);
			try {
				double precision = Task1Solution.evaluateResult(targetFile, solutionFile);
				System.out.println(String.format(
						"LDA + Tfidf: alhpa: %f, beta: %f, topic: %d, iteration: %d, lambda: %f, precisoion: %f", 
						alpha, beta, numTopics, numIterations, lambda, precision));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void setRtm(RelationalTopicModel rtm2) {
		// TODO Auto-generated method stub
		this.rtm = rtm2;
		this.y = rtm.y;
		x = new double[rtm.numOfLinks][ldaIdfModel.lda.numTopics + 1];
	}
	
	protected void _calculateXValues() {
		TObjectIntIterator<String> iterator = rtm.pairIdHash.iterator();
		while(iterator.hasNext()) {
			iterator.advance();
			String key = iterator.key();
			String[] ids = key.split(rtm.BY);
			int v1 = Integer.parseInt(ids[0]);
			int v2 = Integer.parseInt(ids[1]);
			int docIdx = iterator.value();
			for(int i=0; i<x[docIdx].length; i++) {
//				x[docIdx][i] = zbar[v1][i] * zbar[v2][i];
			}
		}
	}
}
