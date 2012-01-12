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
	
	public static void main(String[] args) {
		InstanceList instances = InstanceList.load(new File("dataset/vlc_lectures_train.mallet"));
		LDA lda = new LDA(10);
		lda.estimate (instances, 500, 5000, 0, null, new Randoms());  // should be 1100
		lda.printTopWords (10, true);
	}
}
