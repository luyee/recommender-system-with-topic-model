package rs.model.recommender;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import rs.model.Model;
import rs.model.ir.MalletTfidf;
import rs.model.topics.RelationalTopicModel;
import rs.util.vlc.solution.*;

import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;

public class LdaTfidfCombination extends Model {
	ParallelLdaRecommender ldaModel;
	MalletTfidf tfidfModel;	
	double lambda;	
	
	public LdaTfidfCombination (MalletTfidf tfidf, ParallelLdaRecommender lda) {
		this.ldaModel = lda;
		this.tfidfModel = tfidf;
	}
	
	@Override
	public double getSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		double tfidfSim = tfidfModel.getSim(qdocId, targetDocId);
		double ldaSim = ldaModel.getSim(qdocId, targetDocId);
		double predSim = lambda * tfidfSim + (1-lambda) * 0.2 * ldaSim;
//		double predSim = lambda * ldaSim;
		return predSim;		
	}
	
	@Override
	public InstanceList getTrainingDocuments() {
		// TODO Auto-generated method stub
		return tfidfModel.documents;
	}
	
	public void setLambda(double lam) {
		this.lambda = lam;
	}
		

	public static void main(String[] args) throws IOException {
//			String malletFile = "dataset/vlc/vlc_lectures.all.en.f8.mallet";
//			String queryFile = "dataset/task1_query.en.f8.txt";
//			String targetFile = "dataset/task1_target.en.f8.txt";
//			String queryFile = "dataset/vlc/task1_query.en.f8.n5.txt";
//			String targetFile = "dataset/vlc/task1_target.en.f8.n5.txt";
		String solutionFile = "dataset/task1_solution.en.f8.combine.txt";
		String malletFile = "dataset/vlc/folds/all.0.4189.mallet";

		String queryFile = "dataset/vlc/folds/query.0.csv";
		String targetFile = "dataset/vlc/folds/target.0.csv";
		String simFile = "dataset/vlc/sim5p.csv";
		
		InstanceList documents = InstanceList.load(new File(malletFile));
		MalletTfidf tfidfModel = new MalletTfidf(documents);
//			mt.retrieveTask1Solution(queryFile, solutionFile);
		
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
		
		LdaTfidfCombination model = new LdaTfidfCombination(tfidfModel, ldaModel);
		BasicTask1Solution solver = new Task1Solution(model);
		
		
		double lambda = 0.1;
		for(; lambda<1; lambda = lambda*1.1) {
			model.setLambda(lambda);
			solver.retrieveTask1Solution(queryFile, solutionFile);
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


}
