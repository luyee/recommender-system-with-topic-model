package rs.model.recommender;

/**
 * This class generates a list of candidates using LDA filtering, then rank 
 * the candidates using tfidf. 
 * @author Haibin
 *
 */

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import rs.model.Model;
import rs.model.ir.MalletTfidf;
import rs.model.topics.RelationalTopicModel;
import rs.util.vlc.solution.Task1Solution;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

public class LDAFilterTfidfRankRecommender extends Model {
	ParallelLdaRecommender ldaModel;
	MalletTfidf tfidfModel;	
	double lambda = 0;
	public int filterRank = 100;
	Task1Solution solver;
	
	public void setFilterRank(int filterRank) {
		this.filterRank = filterRank;
	}

	public LDAFilterTfidfRankRecommender (MalletTfidf tfidf, ParallelLdaRecommender lda) {
		this.ldaModel = lda;
		this.tfidfModel = tfidf;
	}
	
	public double getSim(int qdocId, int targetDocId) {
		return 0;
	}
	
	public void initSolver() {
		solver = new Task1Solution(this) {
			public String recommend(String qId) {
				int qdocId = idHash.get(qId);
				
				int test_size = documents.size() - testIndexStart;
				double[] tfidfPredSim = new double[test_size];
				double[] ldaPredSim = new double[test_size]; 
				double[] combinedSim = new double[test_size];
//				Arrays.fill(tfidfPredSim, 0);
				
				for(int i=0; i<test_size; i++) {
					
					tfidfPredSim[i] = tfidfModel.getTfidfSim(qdocId, testIndexStart+i);
					
					combinedSim[i] = lambda * tfidfModel.getTfidfSim(qdocId, testIndexStart+i) 
						+ (1 - lambda) * ldaModel.queryTopicVSM(qdocId, testIndexStart+i);
				}
				int[] idxList = topRankedVideoIndices(combinedSim, filterRank);
				String line = sortFilteredRecommendList(qdocId, tfidfPredSim, idxList);
				if(qdocId == 300) System.out.println(line);
				return line;
			}
		};
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
//			model.retrieveTask1Solution(queryFile, solutionFile, lambda);
			Task1Solution solver = new Task1Solution(model);
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

	@Override
	public InstanceList getTrainingDocuments() {
		// TODO Auto-generated method stub
		return tfidfModel.getTrainingDocuments();
	}

}
