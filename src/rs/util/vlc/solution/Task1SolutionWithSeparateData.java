package rs.util.vlc.solution;

import java.util.Arrays;

import rs.model.Model;
import rs.model.SeparateModel;
import gnu.trove.map.hash.TObjectIntHashMap;
import cc.mallet.types.InstanceList;

public class Task1SolutionWithSeparateData extends BasicTask1Solution {
	InstanceList testDocuments;
	
	public Task1SolutionWithSeparateData() {
	}
	
	public Task1SolutionWithSeparateData(SeparateModel model) {
		super(model);
		this.testDocuments = model.getTestDocuments();
		initTestDocids();
	}
	
	public String recommend(String qId) {		
		int qdocId = idHash.get(qId);
		int test_size = testDocuments.size();
		double[] predSim = new double[test_size];
		Arrays.fill(predSim, 0);
		for(int i=0; i<test_size; i++) {
			predSim[i] = model.getSim(qdocId, i);		
		}
		
		String line = sortRecommendList(qdocId, predSim);
		return line;
	}
	
	protected void initTestDocids() {
		if (testDocuments == null) {
			System.err.println("Document instance should be initialized first.");
			return;
		}
		int testSize = testDocuments.size();
		testDocIds = new String[testSize];
		for(int di = 0; di<testDocuments.size(); di++) {
			testDocIds[di] = (String)testDocuments.get(di).getName();
		}
	}
}
