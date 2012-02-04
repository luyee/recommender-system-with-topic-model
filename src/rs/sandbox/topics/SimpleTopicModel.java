package rs.sandbox.topics;

import java.io.*;
import java.util.regex.Pattern;

import cc.mallet.topics.*;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;
import cc.mallet.pipe.iterator.*;
import cc.mallet.pipe.*;

import rs.util.vlc.*;

public class SimpleTopicModel {
	
	public SimpleTopicModel() {		
		
	}
	
	public static void main(String[] args) throws IOException {
		InstanceList instances = InstanceList.load(new File("dataset/cora/cora.mallet"));
		ParallelTopicModel lda = new ParallelTopicModel(8, 0.1*8, 0.1);
		lda.addInstances(instances);
		lda.setNumIterations(2000);
		lda.estimate();
		lda.printTopWords(System.out, 10, false);
	}
}
