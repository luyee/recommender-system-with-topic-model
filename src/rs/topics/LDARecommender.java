package rs.topics;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

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
		int numOfTopics = 20;
		LDAModel lda = new LDAModel(numOfTopics);
		
		String malletFile = "dataset/vlc_lectures.all.en.f8.mallet";
		String simFile = "dataset/vlc/sim5p.csv";
		String solutionFile = "dataset/vlc/task1_solution.en.f8.lm.txt";
		String queryFile = "dataset/task1_query.en.f8.txt";
		String targetFile = "dataset/task1_target.en.f8.txt";
		
		int numOfTopic = 20;
		int numIter = 1000;
		
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

