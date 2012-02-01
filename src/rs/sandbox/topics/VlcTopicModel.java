package rs.sandbox.topics;

import java.io.*;
import java.util.*;

import cc.mallet.types.*;
import cc.mallet.topics.*;

public class VlcTopicModel {
	public static void main(String[] args) throws IOException {
		InstanceList instances = InstanceList.load(new File("dataset/vlc_lectures_train.mallet.2"));
		
		int numTopics = 50;
		ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.02);
		
		model.addInstances(instances);
		model.setNumThreads(4);
		model.setNumIterations(50);
		model.estimate();
		
		Alphabet dataAlphabet = instances.getAlphabet();
		FeatureSequence tokens = (FeatureSequence) model.getData().get(36).instance.getData();
		
		LabelSequence topics = model.getData().get(36).topicSequence;
		
		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		for (int position = 0; position < tokens.getLength(); position++) {
			out.format("%s-%d", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
		}
		System.out.println(out);
		
		// Estimate the topic distribution of an instance,
		// given the current Gibbs state.
		double[] topicDistribution = model.getTopicProbabilities(36);
		
		// Get an array of sorted sets of word ID/count pairs
		ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
		
		// Show top 5 words in topics with proportions for the document
		for (int topic = 0; topic < numTopics; topic++) {
			Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
			
			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t%.3f", topic, topicDistribution[topic]);
			int rank = 0;
			while (iterator.hasNext() && rank < 5) {
				IDSorter idCountPair = iterator.next();
				out.format("%s (%.0f ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
				rank++;
			}
			System.out.println(out);
		}
		
		// Create a new instance with high probability of topic 0
		StringBuilder topicZeroText = new StringBuilder();
		Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();
		
		int rank = 0;
		while (iterator.hasNext() && rank < 5) {
			IDSorter idCountPair = iterator.next();
			topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
			rank++;
		}
		
		// Create a new instance named "test instance" with empty target and source fields.
		InstanceList testing = new InstanceList(instances.getPipe());
		testing.addThruPipe(new Instance(topicZeroText.toString(), null, "test instance", null));
		
		TopicInferencer inferencer = model.getInferencer();
		double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
		System.out.println("0\t" + testProbabilities[0]);
	}
}
