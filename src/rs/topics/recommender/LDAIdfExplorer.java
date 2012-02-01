package rs.topics.recommender;

import rs.util.vlc.Task1Solution;
import cc.mallet.topics.*;
import cc.mallet.types.*;
import java.io.*;

public class LDAIdfExplorer extends TopicIdfRecommender {
	ParallelTopicModel lda;
	
	LDAIdfExplorer(InstanceList documents) {
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
	
	public static void testParameter(InstanceList documents, String queryFile, String solutionFile,
			int numTopics, int numIterations, double alpha, double beta) throws IOException {
		LDAIdfExplorer explorer = new LDAIdfExplorer(documents);
		
		double alphaSum = alpha * numTopics;
		ParallelTopicModel lda = new ParallelTopicModel(numTopics, alphaSum, beta);
		lda.addInstances(documents);
		lda.setNumThreads(4);
		lda.setNumIterations(numIterations);
		
//		String modelFile = String.format("dataset/lda.model.dat.%f.%f.%d.%d", alpha, beta, numTopics, numIterations);
//		lda.setSaveSerializedModel(numIterations, modelFile);
		lda.setTopicDisplay(1000, 5);
		lda.printLogLikelihood = false;
		lda.estimate();
		System.out.println("LDA parameter, alphaSum: " + lda.alphaSum + ", beta: " + lda.beta);
		
		explorer.setPtm(lda);
		explorer.calculateProb();

		explorer.retrieveTask1Solution(queryFile, solutionFile);
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
//		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
		String malletFile = "dataset/vlc_lectures.all.5000term.mallet";
		String simFile = "dataset/sim.csv";
		String queryFile = "dataset/task1_query.en.f8.txt";
		String targetFile = "dataset/task1_target.en.f8.txt";
		String solutionFile = "dataset/task1_solution.en.f8.txt";
		String testResultFile = "dataset/ldaexplorer.2.txt";
		

//		BufferedWriter writer = new BufferedWriter(new FileWriter(testResultFile));
		
		InstanceList documents = InstanceList.load(new File(malletFile));
		int numIterations = 2000;
		double alpha = 0.005;
		for(int i=0; i<10; i++) {
			alpha *= 2;
			int topic = 10;
			for (int m=0; m<6; m++) {
				topic *= 2;
				double beta = 0.00005;
//				for (int n=0; n<2; n++) {
					beta *= 2;
//					String solutionFile = String.format(
//							"dataset/task1_solution.en.f8.%f.%f.%d.%d.txt", alpha, beta, topic, numIterations);
					LDAIdfExplorer.testParameter(documents, queryFile, solutionFile, topic, numIterations, alpha, beta);
					try {
						double precision = Task1Solution.evaluateResult(targetFile, solutionFile);
						System.out.println(String.format(
								"TopicIdf: alhpa: %f, beta: %f, topic: %d, iteration: %d, precisoion: %f", 
								alpha, beta, topic, numIterations, precision));
//						writer.write(String.format(
//								"alhpa: %f, beta: %f, topic: %d, iteration: %d, precisoion: %f", 
//								alpha, beta, topic, numIterations, precision));
//						writer.newLine();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//				}
			}
		}
//		writer.flush();
//		writer.close();
	}
}
