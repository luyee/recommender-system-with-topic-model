/**
 * This class will do regression over LDA first, then try to calculate linklikelihood 
 * of sampled test set. Logistic regression is used here as in primertm, parameters are 
 * also calculated like PrimeRtm.
 */
package rs.text.link.likelihood;

import cc.mallet.topics.*;
import cc.mallet.types.*;
import cc.mallet.util.Randoms;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.*;
import java.util.*;

import rs.text.model.*;
import rs.types.PairedInfo;

public class LDALinkLikelihood {
//	ParallelTopicModel lda;
	LDAModel lda;
	
	public final static String BY = RelationalTopicModel.BY; 

	public TObjectIntHashMap<String> idHash;
	public TObjectIntHashMap<String> pairIdHash; 	// v1xv2 -> int
	public ArrayList<PairedInfo> links;
	public int numOfLinks;
	
	double[][] x;
	double[][] zbar;
	double[] eta;
	
	public LDALinkLikelihood(LDAModel lda) {
		this.lda = lda;
		eta = new double[lda.numTopics]; 
	}
	
	/**
	 * Calculate loglikhood of sampled links.
	 * @param model
	 * @param samples
	 * @return
	 */
	public double linkLogLikelihood(ArrayList<Edge> samples) {
		double loglikelihood = 0;
		Iterator<Edge> iterator = samples.iterator();
		while(iterator.hasNext()) {
			Edge edge = iterator.next();
			int d1 = edge.node1;
			int d2 = edge.node2;
			double sum = 0;
			double[] p1 = zbar[d1];
			double[] p2 = zbar[d2];
			for(int k = 0; k < lda.numTopics; k++) {
				sum += eta[k] * p1[k] * p2[k];
			}
			loglikelihood += sum ;//+ Math.log(Util.sigmoid(0-sum));
		}
		return loglikelihood;
	}
	
	public double[] linkSimilarity(ArrayList<Edge> samples) {
		double[] sim = new double[samples.size()];
		double[] eta = this.eta;
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
			for (int k=0; k<lda.numTopics; k++) {
				sum += eta[k] * zbar[d1][k] * zbar[d2][k];
			}
			sum -= meta;
			sim[eIdx] = Math.exp(sum);
			eIdx++;
		}
		return sim;
	}
	
	
	private void _calculateZbar() {
		// TODO Auto-generated method stub
		if(lda == null) {
			System.out.println("Should run lda estimation first.");
			System.exit(1);
			return;
		}
		if (zbar == null) {
			zbar = new double[lda.ilist.size()][lda.numTopics];
		}
		for(int di=0; di<lda.ilist.size(); di++) {
			FeatureSequence fs = (FeatureSequence)lda.ilist.get(di).getData();
			int docLen = fs.getLength();
			for(int ti=0; ti<lda.numTopics; ti++) {
				zbar[di][ti] = (lda.docTopicCounts[di][ti]+lda.alpha) / ((double)docLen + lda.alpha * lda.numTopics);
			}
		}
	}
	
	private void _calculateXValue() {
		if(lda == null) {
			System.out.println("Should run lda estimation first.");
			System.exit(1);
			return;
		}
		if (x == null) {
			x = new double[numOfLinks][lda.numTopics];
		}

		TObjectIntIterator<String> iterator = pairIdHash.iterator();
		while(iterator.hasNext()) {
			iterator.advance();
			String key = iterator.key();
			String[] ids = key.split(BY);
			int v1 = Integer.parseInt(ids[0]);
			int v2 = Integer.parseInt(ids[1]);
			int docIdx = iterator.value();
			for(int i=0; i<lda.numTopics; i++) {
				x[docIdx][i] = zbar[v1][i] * zbar[v2][i]; 
			}
		}
	}
	
	
	/**
	 * Calculate the eta value.
	 */
	public void params(InstanceList documents) {
		_calculateZbar();
		_calculateXValue();
		double lambda = (double)numOfLinks /(double) (documents.size() * (documents.size() -1)/2);
		double[] p = new double[lda.numTopics];
		Arrays.fill(p, 0);
		for(int i=0; i<x.length; i++) {
			for(int t=0; t<lda.numTopics; t++) {
				p[t] += x[i][t];
			}
		}
		for(int t=0; t<lda.numTopics; t++) {
			double temp = p[t] / (p[t] + lambda * lda.alpha * lda.alpha * documents.size() * (documents.size() -1) /2);
			eta[t] = Math.log(temp/(1-temp));
		}
	}


	public void readLinksUsingRtm(String malletFile, String simFile) throws IOException {
		PrimeRtm rtm = new PrimeRtm(lda.numTopics);
		rtm.initFromFile(malletFile, simFile);
		
		this.idHash = rtm.idHash;
		this.pairIdHash = rtm.pairIdHash;
		this.links = rtm.links;
		this.numOfLinks = rtm.numOfLinks;
	}
	
	public static void main(String[] args) throws IOException {
		String malletFile = "dataset/cora/cora.mallet";
		String linkTrain = "dataset/cora/links.train.txt";
		String linkTest = "dataset/cora/links.test.txt";
		
		int numOfTopics = 4;
		int numIterations = 100;
		double alpha = 0.125;
//		double beta = 0.0016;
		for(int i=0; i<10; i++) {
			numOfTopics *= 2;
			double beta = 0.0008;
//			for(; beta<1; beta*=2) {
				LDAModel lda = new LDAModel(numOfTopics, 1, beta);
				InstanceList documents = InstanceList.load(new File(malletFile));
				lda.estimate(documents, numIterations, 0, 0, null, new Randoms());
		
		//		lda.addInstances(documents);
		//		lda.setNumIterations(numIterations);
		//		lda.setNumIterations(numIterations);
		//		lda.setSymmetricAlpha(false);
		//		lda.estimate();
				
				LDALinkLikelihood lll = new LDALinkLikelihood(lda);
				lll.readLinksUsingRtm(malletFile, linkTrain);
				
				ArrayList<Edge> samples = Util.importEdges(linkTest, lll.idHash);
				lll.params(documents);
		
				double likelihood = lll.linkLogLikelihood(samples);
				System.out.println("numoftopics: " + numOfTopics + ", beta: " + beta + ", " +likelihood);
//			}
		}
	}
}
