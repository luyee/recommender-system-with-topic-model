package rs.model.recommender;

import rs.model.Model;
import rs.model.ir.MalletTfidf;
import cc.mallet.types.InstanceList;

public class RtmTfidfCombinaton extends Model {
	
	RtmRecommender rtmModel;
	MalletTfidf tfidfModel;	
	double lambda;	
	
	public RtmTfidfCombinaton (MalletTfidf tfidf, RtmRecommender rtm) {
		this.rtmModel = rtm;
		this.tfidfModel = tfidf;
	}
	
	@Override
	public double getSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		double tfidfSim = tfidfModel.getSim(qdocId, targetDocId);
		double rtmSim = rtmModel.queryTopicVSM(qdocId, targetDocId);
		double predSim = lambda * tfidfSim + (1-lambda) * rtmSim;
		return predSim;		
	}
	
	@Override
	public InstanceList getTrainingDocuments() {
		// TODO Auto-generated method stub
		return tfidfModel.documents;
	}
	
	public void setLambda(double lam) {
		this.lambda = lam;
	}
}
