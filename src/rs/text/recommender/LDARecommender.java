package rs.text.recommender;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import rs.text.topics.LDAModel;
import rs.util.vlc.Task1Solution;

import cc.mallet.topics.*;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
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
		int numOfTopics = 160;
		int numIter = 2000;
		double alpha = 0.0016;
		double beta = 0.0048;
		LDAModel lda = new LDAModel(numOfTopics, alpha*numOfTopics, beta);
		
//		String malletFile = "dataset/vlc/vlc_lectures.all.en.f8.mallet";
//		String malletFile = "dataset/vlc/vlc_lectures.all.title.en.f2.mallet";
		String simFile = "dataset/vlc/sim_0p_10n.csv";
		String solutionFile = "dataset/vlc/task1_solution.en.f8.lm.txt";
//		String queryFile = "dataset/task1_query.en.f8.txt";
//		String targetFile = "dataset/task1_target.en.f8.txt";
//		String queryFile = "dataset/vlc/task1_query.en.f8.n5.txt";
//		String targetFile = "dataset/vlc/task1_target.en.f8.n5.txt";

		String malletFile = "dataset/vlc/folds/all.0.4189.mallet";

		String queryFile = "dataset/vlc/folds/query.0.csv";
		String targetFile = "dataset/vlc/folds/target.0.csv";
		
//		String queryFile = "dataset/vlc/task1_query.en.title.f2.txt";
//		String targetFile = "dataset/vlc/task1_target.en.title.f2.txt";

		InstanceList documents = InstanceList.load(new File(malletFile));
		lda.estimate(documents, numIter, 0, 0, null, new Randoms());
		
		LDARecommender tester;
		
		tester = new LDARecommender(lda);
		tester.calculateProb();
		
		tester.retrieveTask1Solution(queryFile, solutionFile);
		double precision;
		try {
			precision = Task1Solution.evaluateResult(targetFile, solutionFile);	
			System.out.println(String.format(
					"LDARecommender: iteration: %d, precisoion: %f", 
					 numIter, precision));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}