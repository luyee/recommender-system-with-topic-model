package rs.text.separate;

import java.util.Arrays;

import gnu.trove.map.hash.TObjectIntHashMap;
import rs.text.recommender.MalletMle;
import rs.util.vlc.Task1Solution;
import cc.mallet.types.*;

public class SeparateMalletMle {
	InstanceList training;
	InstanceList queryDocs;
	MalletMle mle;
	public Task1Solution solver;
	public TObjectIntHashMap<String> queryIdHash;
	
	SeparateMalletMle(InstanceList train, InstanceList test, InstanceList all) {
		this.training = train;
		this.queryDocs = queryDocs;
		mle = new MalletMle(training);
		initSolver(all);
	}
	
	/**
	 * Init idHahs according to documents, here it is an instance list.
	 */
	private void initIdHash() {
		InstanceList documents = queryDocs;
		if (documents == null) {
			System.err.println("Document instance should be initialized first.");
			return;
		}
		if(queryIdHash == null) {
			queryIdHash = new TObjectIntHashMap<String>();
		}
		for(int i=0; i<documents.size(); i++) {
			Instance doc = documents.get(i);
			String vId = (String) doc.getName();
			queryIdHash.put(vId, i);
		}
	}
	
	public void initSolver(InstanceList all) {
		solver = new Task1Solution(all) {
			public String recommend(String qId) {
				int qdocId = queryIdHash.get(qId);
				
				int test_size = documents.size() - testIndexStart;
				double[] predSim = new double[test_size];
				Arrays.fill(predSim, 0);
				for(int i=0; i<test_size; i++) {
					predSim[i] = getMleSim(qdocId, i);
//					predSim[i] = getTwidfSim(qdocId, testIndexStart+i);
				}
				String line = sortRecommendList(qdocId, predSim);
				return line;
			}
		};
	}
	
	private double getMleSim(int qdocId, int i) {
		// TODO Auto-generated method stub
		FeatureSequence fs = (FeatureSequence)queryDocs.get(qdocId).getData();
		
		return 0;
	}
}
