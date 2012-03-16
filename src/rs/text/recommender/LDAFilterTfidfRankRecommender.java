package rs.text.recommender;

/**
 * This class generates a list of candidates using LDA filtering, then rank 
 * the candidates using tfidf. 
 * @author Haibin
 *
 */

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import rs.text.ir.MalletTfidf;
import rs.text.topics.RelationalTopicModel;
import rs.util.vlc.Task1Solution;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

public class LDAFilterTfidfRankRecommender {
	ParallelLdaRecommender ldaModel;
	MalletTfidf tfidfModel;	
	double lambda = 0;
	public Task1Solution solver;
	public static final int testIndexStart = RelationalTopicModel.testIndexStart;
	public int filterRank = 100;
	
	public void setFilterRank(int filterRank) {
		this.filterRank = filterRank;
	}

	public LDAFilterTfidfRankRecommender (MalletTfidf tfidf, ParallelLdaRecommender lda) {
		this.ldaModel = lda;
		this.tfidfModel = tfidf;
	}
	
	public void initSolver() {
		solver = new Task1Solution(tfidfModel.documents) {
			public String recommend(String qId) {
				int qdocId = idHash.get(qId);
				
				int test_size = documents.size() - testIndexStart;
				double[] tfidfPredSim = new double[test_size];
				double[] ldaPredSim = ldaModel.queryTopicVSM(qdocId, testIndexStart, test_size); 
				double[] combinedSim = new double[test_size];
//				Arrays.fill(tfidfPredSim, 0);
				
				for(int i=0; i<test_size; i++) {
					
					tfidfPredSim[i] = tfidfModel.getTfidfSim(qdocId, testIndexStart+i);
					
					combinedSim[i] = lambda * tfidfModel.getTfidfSim(qdocId, testIndexStart+i) 
						+ (1 - lambda) * ldaPredSim[i];
				}
				int[] idxList = topRankedVideoIndices(combinedSim, filterRank);
				String line = sortFilteredRecommendList(qdocId, tfidfPredSim, idxList);
				if(qdocId == 300) System.out.println(line);
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
		String solutionFile = "dataset/task1_solution.en.f8.LDAfilter.txt";
		
		String malletFile = "dataset/vlc/vlc_lectures.all.en.f8.mallet";
		String simFile = "dataset/vlc/sim_0p_10n.csv";
		
		String queryFile = "dataset/vlc/task1_query.en.f8.n5.txt";
		String targetFile = "dataset/vlc/task1_target.en.f8.n5.txt";
		
		InstanceList documents = InstanceList.load(new File(malletFile));
		MalletTfidf tfidfModel = new MalletTfidf(documents);
		
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
		
		ParallelLdaRecommender ldaModel = new ParallelLdaRecommender(documents);
		ldaModel.setPtm(lda);
		ldaModel.calculateProb();

		LDAFilterTfidfRankRecommender model = new LDAFilterTfidfRankRecommender(tfidfModel, ldaModel);
		
		int rank = 50;
		double lambda = 0.1;
		for(; lambda<1; lambda = lambda + 0.05) {
//		for(rank = 30; rank < 100; rank += 10) {
			model.setFilterRank(rank);
			model.retrieveTask1Solution(queryFile, solutionFile, lambda);
			try {
				double precision = Task1Solution.evaluateResult(targetFile, solutionFile);
				System.out.println(String.format(
						"CombinedSim filtered %d Tfidf rank: alhpa: %f, beta: %f, topic: %d, iteration: %d, lambda: %f, precisoion: %f", 
						rank, alpha, beta, numTopics, numIterations, lambda, precision));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
