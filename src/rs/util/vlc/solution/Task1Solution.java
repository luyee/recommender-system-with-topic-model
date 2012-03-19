package rs.util.vlc.solution;

import java.util.Arrays;

import rs.model.Model;
import gnu.trove.map.hash.TObjectIntHashMap;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class Task1Solution extends BasicTask1Solution {
	
//	public static final int testIndexStart = 5236;
	public static final int testIndexStart = 4189;

	public Task1Solution() {
	}
	
	public Task1Solution(Model model) {
		super(model);
		initTestDocids();
	}

	/**
	 * Init the string array of test documents id.
	 */
	protected void initTestDocids() {
		if (documents == null) {
			System.err.println("Document instance should be initialized first.");
			return;
		}
		
		int testSize = documents.size() - testIndexStart;
		testDocIds = new String[testSize];
		for(int di = 0; di<testSize; di++) {
			testDocIds[di] = (String)documents.get(di+testIndexStart).getName();
		}
	}
	
	public String recommend(String qId) {
		int qdocId = idHash.get(qId);
		
		int test_size = documents.size() - testIndexStart;
		double[] predSim = new double[test_size];
		Arrays.fill(predSim, 0);
		for(int i=0; i<test_size; i++) {
			predSim[i] = model.getSim(qdocId, testIndexStart+i);		
		}
		
//		String line = sortRecommendList2(qdocId, predSim);
		int[] idxList = topRankedVideoIndices(predSim, LIST_SIZE);
		String line = sortFilteredRecommendList(qdocId, predSim, idxList);
		return line;
	}
}
