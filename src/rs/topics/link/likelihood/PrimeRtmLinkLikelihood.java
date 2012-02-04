package rs.topics.link.likelihood;

import java.io.IOException;
import java.util.*;

import cc.mallet.util.Randoms;
import rs.topics.model.*;

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
	
	public static void main(String[] args) throws IOException {
		String malletFile = "dataset/cora/cora.mallet";
		String linkTrain = "dataset/cora/links.train.txt";
		String linkTest = "dataset/cora/links.test.txt";
		
		int numOfTopics = 4;
		int numIterations = 10;
		double alpha = 0.125;
//		double beta = 0.0016;
		for(int i=0; i<10; i++) {
			numOfTopics *= 2;
			double beta = 0.0008;
			PrimeRtm rtm = new PrimeRtm(numOfTopics, 1, beta);
			rtm.initFromFile(malletFile, linkTrain);
			rtm.estimate(numIterations, new Randoms());
			
			ArrayList<Edge> samples = Util.importEdges(linkTest, rtm.idHash);
			double likelihood = linkLogLikelihood(rtm, samples); 
			System.out.println("numoftopics: " + numOfTopics + ", beta: " + beta + ", " +likelihood);
		}
	}
}
