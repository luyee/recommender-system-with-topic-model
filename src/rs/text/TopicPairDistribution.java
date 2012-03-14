package rs.text;

import gnu.trove.iterator.TObjectIntIterator;

import java.io.File;
import java.io.IOException;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;

import rs.text.model.*;
import rs.text.recommender.MalletTfidf;
import rs.util.vlc.Util;

public class TopicPairDistribution {
	public static void main(String[] args) throws IOException {
		String malletFile = "dataset/vlc/vlc_train.en.f8.mallet";
		String linkFile = "dataset/vlc/sim_0p_100n.csv";
		int numIter = 1000;
		int numTopic = 10;
		double alpha = 0.1;
		double beta  = 0.1;
		LDAModel lda = new LDAModel(numTopic, alpha * numTopic, beta);
		InstanceList documents = InstanceList.load(new File(malletFile));
		lda.estimate(documents, numIter, 0, 0, null, new Randoms());
		double[][] theta = new double[lda.ilist.size()][lda.numTopics];
		for(int i=0; i<lda.ilist.size(); i++) {
			FeatureSequence fs = (FeatureSequence)lda.ilist.get(i).getData();
			int docLen = fs.size();
			for (int j=0; j<lda.numTopics; j++) {
				theta[i][j] = (double)(lda.docTopicCounts[i][j] + lda.alpha) /
									(double) (docLen + lda.alpha);
			}
		}
		
		MalletTfidf tfidf = new MalletTfidf(documents);
		
		RelationalTopicModel rtm = new RelationalTopicModel(numTopic);
		rtm.initFromFile(malletFile, linkFile);
		TObjectIntIterator<String> iterator = rtm.pairIdHash.iterator();
		while(iterator.hasNext()) {
			iterator.advance();
			String key = iterator.key();
			String[] ids = key.split(rtm.BY);
			int v1 = Integer.parseInt(ids[0]);
			int v2 = Integer.parseInt(ids[1]);
			double freqSim = rtm.getPairedSim(v1, v2) * 10;
			double vsmSim = Util.cosineProduct(theta[v1], theta[v2]);
			double tfidfSim = tfidf.getTfidfSim(v1, v2);
			
			System.out.println(lda.getInstanceList().get(v1).getName().toString() + "\t" +
							   lda.getInstanceList().get(v2).getName().toString() + "\t" +
							   freqSim + "\t" + vsmSim + "\t" + tfidfSim);
		}
	}
}
