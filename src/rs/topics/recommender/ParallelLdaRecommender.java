package rs.topics.recommender;

import java.io.File;
import java.io.IOException;

import rs.util.vlc.Task1Solution;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;

public class ParallelLdaRecommender extends TopicRecommender{
	ParallelTopicModel lda;
	
	ParallelLdaRecommender(InstanceList documents) {
		super(documents);
	}

	public void setPtm(ParallelTopicModel p) {
		this.lda = p;
	}
	
	public void calculateProb() {
		phi = new double[lda.numTypes][lda.numTopics];
		theta = new double[lda.data.size()][];
		
		calculateTopicWordWeights();
		for(int i=0; i<lda.data.size(); i++) {
			theta[i] = lda.getTopicProbabilities(i);
		}
	}
	
	public void calculateTopicWordWeights() {
		int numTopics = lda.numTopics;
		int numTypes = lda.numTypes;
		int[][] typeTopicCounts = lda.typeTopicCounts;
		int[] tokensPerTopic = lda.tokensPerTopic;
		double beta = lda.beta;
		int topicMask = lda.topicMask;
		int topicBits = lda.topicBits;
		
		for (int topic = 0; topic < numTopics; topic++) {
			for (int type = 0; type < numTypes; type++) {
				int[] topicCounts = typeTopicCounts[type];
				double weight = beta;
				int index = 0;
				while (index < topicCounts.length &&
					   topicCounts[index] > 0) {
					int currentTopic = topicCounts[index] & topicMask;
					if (currentTopic == topic) {
						weight += topicCounts[index] >> topicBits;
						break;
					}
					index++;
				}
				phi[type][topic] = weight / (double) (tokensPerTopic[topic] + beta);
			}
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		String malletFile = "dataset/vlc/vlc_lectures.all.en.f8.mallet";
//		String malletFile = "dataset/vlc_lectures.all.5000term.mallet";
		String simFile = "dataset/vlc/sim5p.csv";
		String solutionFile = "dataset/vlc/task1_solution.en.f8.lm.txt";
//		String queryFile = "dataset/task1_query.en.f8.txt";
//		String targetFile = "dataset/task1_target.en.f8.txt";
		String queryFile = "dataset/vlc/task1_query.en.f8.n5.txt";
		String targetFile = "dataset/vlc/task1_target.en.f8.n5.txt";
		
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
		
		ParallelLdaRecommender tester = new ParallelLdaRecommender(documents);
		tester.setPtm(lda);
		tester.calculateProb();
		
		tester.retrieveTask1Solution(queryFile, solutionFile);
		
		double precision;
		try {
			precision = Task1Solution.evaluateResult(targetFile, solutionFile);	
			System.out.println(String.format(
					"ParallelLdaRecommender: iteration: %d, precisoion: %f", 
					numIterations, precision));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
