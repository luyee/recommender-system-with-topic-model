package rs.model.ir.separate;

import cc.mallet.types.InstanceList;
import rs.model.SeparateModel;
import rs.model.ir.*;

public class SeparateMalletClusterMle extends SeparateModel{
	MalletMleCluster training;
	MalletMle test;
		
	@Override
	public InstanceList getTestDocuments() {
		// TODO Auto-generated method stub
		return test.getTrainingDocuments();
	}

	@Override
	public double getSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public InstanceList getTrainingDocuments() {
		// TODO Auto-generated method stub
		return training.getTrainingDocuments();
	}

}
