package rs.model.link.likelihood;

import java.io.File;
import java.io.IOException;
import java.util.*;

import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;
import rs.model.topics.*;

public class PrimeRtmLinkLikelihood {
	/**
	 * Calculate loglikhood of sampled links.
	 * @param model
	 * @param samples
	 * @return
	 */
	public static double linkLogLikelihood(PrimeRtm model, ArrayList<Edge> samples) {
		double loglikelihood = 0;
		double[] eta = model.eta;
		Iterator<Edge> iterator = samples.iterator();
		while(iterator.hasNext()) {
			Edge edge = iterator.next();
			int d1 = edge.node1;
			int d2 = edge.node2;
			double sum = 0;
			for(int k = 0; k < model.numOfTopics; k++) {
				sum += eta[k] * model.zbar[d1][k] * model.zbar[d2][k];
			}
			loglikelihood += sum + Math.log(Util.sigmoid(0-sum));
		}
		return loglikelihood;
	}
	
	public static double[] linkSimilarity(PrimeRtm model, ArrayList<Edge> samples) {
		double[] sim = new double[samples.size()];
		double[] eta = model.eta;
		double meta = 0;
		for(int i=0; i<eta.length; i++) {
			if(eta[i] > meta)
				meta = eta[i];
		}
		int eIdx = 0;
		Iterator<Edge> iterator = samples.iterator();
		while(iterator.hasNext()) {
			Edge edge = iterator.next();
			int d1 = edge.node1;
			int d2 = edge.node2;
			double sum = 0;
			for (int k=0; k<model.numOfTopics; k++) {
				sum += eta[k] * model.zbar[d1][k] * model.zbar[d2][k];
			}
			sum -= meta;
			sim[eIdx] = Math.exp(sum);
			eIdx++;
		}
		return sim;
	}
	
	public static void main(String[] args) throws IOException {
//		String malletFile = "dataset/cora/cora.mallet";
//		String linkTrain = "dataset/cora/fold/links.train.txt.1";
//		String linkTest = "dataset/cora/fold/links.test.txt.1";
//		
//		int numOfTopics = 8;
//		int numIter = 20;
//		int sampleIter = 40;
//		double alpha = 0.1;
//		double beta = 0.1;
//
//		PrimeRtm rtm = new PrimeRtm(numOfTopics, alpha*numOfTopics, beta);
//		rtm.initFromFile(malletFile, linkTrain);
//		rtm.estimate2(numIter, new Randoms(), sampleIter);
//		ArrayList<Edge> samples = Util.importEdges(linkTest, rtm.idHash);
//		double[] rtmsim = linkSimilarity(rtm, samples);
//		double avgSim = average(rtmsim);
//		System.out.println("\nalpha: " + alpha + ", beta: " + beta + 
//				", topics: " + numOfTopics);
//		System.out.println("RTM average sim: " + avgSim);
		
		String malletFile = "dataset/cora/cora.mallet";
		String trainFile = "dataset/cora/fold/links.train.txt";
		String testFile = "dataset/cora/fold/links.test.txt";
		
		int numOfTopics = 5;
		int numIter = 20;
		int sampleIter = 500;
		double alpha = 0.1;
		double beta = 0.1;
//		for(; alpha<0.2; alpha = alpha * 1.5) {
			for(; numOfTopics<25; numOfTopics+=5) {
//				for(; beta<0.2; beta = beta *1.5) {
					for(int fold=1; fold<=5; fold++) {
						String trainSimFile = trainFile + "." + fold;
						String testSimFile = testFile + "." + fold;
						for(int i=0; i<5; i++) {
							PrimeRtm rtm = new PrimeRtm(numOfTopics, alpha*numOfTopics, beta);
							rtm.initFromFile(malletFile, trainSimFile);
							rtm.estimate2(numIter, new Randoms(), sampleIter);
							ArrayList<Edge> samples = Util.importEdges(testSimFile, rtm.idHash);
							double[] rtmsim = linkSimilarity(rtm, samples);
							double avgSim = average(rtmsim);
							System.out.println("\nalpha: " + alpha + ", beta: " + beta + 
									", topics: " + numOfTopics + ", fold: " + fold + ", expNum: " + i);
							System.out.println("RTM average sim: " + avgSim);
							System.out.println("RTM sim loglikehood: " + loglikelihood(rtmsim));

							LDAModel lda = new LDAModel(numOfTopics, alpha*numOfTopics, beta);
							InstanceList documents = InstanceList.load(new File(malletFile));
							lda.estimate(documents, 2000, 0, 0, null, new Randoms());
							LDALinkLikelihood lll = new LDALinkLikelihood(lda);
							lll.readLinksUsingRtm(malletFile, trainSimFile);
							lll.params(documents);
							double[] ldaSim = lll.linkSimilarity(samples);
							double ldaAvgSim = average(ldaSim);
							System.out.println("LDA average sim: " + ldaAvgSim);			
							System.out.println("LDA sim loglikehood: " + loglikelihood(ldaSim));
						}
					}
				}
//			}
//		}
	}

	private static double average(double[] sim) {
		double avgSim = 0;
		for(int i=0; i<sim.length; i++) {
			avgSim += sim[i];
		}
		avgSim = avgSim/sim.length;
		return avgSim;
	}
	
	private static double loglikelihood(double[] sim) {
		double likelihood = 0;
		for(int i=0; i<sim.length; i++) {
			likelihood += Math.log(sim[i]);
		}
		return likelihood;
	}
}
