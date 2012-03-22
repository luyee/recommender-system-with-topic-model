package rs.model.ir.combination;

import cc.mallet.types.InstanceList;
import rs.model.Model;
import rs.model.ir.*;

public class TfidfMleCombine extends Model {
	public MalletTfidf tfidf;
	public MalletMle mle;
	public double lambda;
	public double mleLambda;
	
	public TfidfMleCombine(MalletTfidf tfidf, MalletMle mle) {
		this.tfidf = tfidf;
		this.mle = mle;
	}
	
	public TfidfMleCombine(InstanceList documents) {
		tfidf = new MalletTfidf(documents);
		mle = new MalletMle(documents);
	}
	
	public void setParam(double lambda, double mleLambda) {
		this.lambda = lambda;
		this.mleLambda = mleLambda;
	}

	@Override
	public double getSim(int qdocId, int targetDocId) {
		// TODO Auto-generated method stub
		double sim = lambda * tfidf.getSim(qdocId, targetDocId) + 
						(1-lambda) * mle.getTermProbVSM(qdocId, targetDocId); 
		return sim;
	}

	@Override
	public InstanceList getTrainingDocuments() {
		// TODO Auto-generated method stub
		return tfidf.getTrainingDocuments();
	}
	
}
