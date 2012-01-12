package rs.topics;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import rs.util.vlc.Task1Solution;

import cc.mallet.topics.*;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.Randoms;

public class LDARecommender extends TopicRecommender{

	LDAModel lda;
	
	LDARecommender(LDAModel p) {
		super(p.getInstanceList());
		this.lda = p;
	}

	public void calculateProb() {
		phi = new double[lda.numTypes][lda.numTopics];
		theta = new double[lda.ilist.size()][lda.numTopics];
		
		for(int i=0; i<lda.numTypes; i++) 
			for (int j=0; j<lda.numTopics; j++) {
				phi[i][j] = (double) (lda.typeTopicCounts[i][j] + lda.beta) / 
								(double) (lda.tokensPerTopic[j]+lda.beta);
			}
		for(int i=0; i<lda.ilist.size(); i++) {
			FeatureSequence fs = (FeatureSequence)lda.ilist.get(i).getData();
			int docLen = fs.size();
			for (int j=0; j<lda.numTopics; j++) {
				theta[i][j] = (double)(lda.docTopicCounts[i][j] + lda.alpha) /
									(double) (docLen + lda.alpha);
			}
		}
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
//		ParallelTopicModel ptm = new ParallelTopicModel(100, 50, 0.01);
		String objectFile = "dataset/rtm.100.3000.dat";
		String solutionFile = "dataset/task1_solution.en.f8.50.3000.lm.txt";
		String queryFile = "dataset/task1_query.en.f8.txt";
		ObjectInputStream input = new ObjectInputStream(new FileInputStream(objectFile));
		RelationalTopicModel rtm = (RelationalTopicModel) input.readObject();
		
//		ptm.addInstances(rtm.documents);
//		ptm.setNumIterations(3000);
//		ptm.setSaveSerializedModel(1000, "dataset/lda.model.dat");
//		ptm.setTopicDisplay(100, 5);
//		ptm.estimate();

//		LDAModel lda = new LDAModel(50, 5, 0.01);
//		lda.estimate(rtm.documents, 3001, 200, 1000, "dataset/lda.model.dat", new Randoms());
//		lda.readObject(new ObjectInputStream(new FileInputStream(ldaFile)));

		String ldaFile = "dataset/lda.model.dat.3000";
		input = new ObjectInputStream(new FileInputStream(ldaFile));
		LDAModel lda = (LDAModel) input.readObject();
		
		LDARecommender tester;
		
		tester = new LDARecommender(lda);
		tester.calculateProb();
		tester.recommendSolution(queryFile, solutionFile);
	}
}

