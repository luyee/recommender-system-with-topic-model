package rs.model;

import cc.mallet.types.InstanceList;

public abstract class Model {

	public abstract double getSim(int qdocId, int targetDocId);
	
	public abstract InstanceList getTrainingDocuments();

}
