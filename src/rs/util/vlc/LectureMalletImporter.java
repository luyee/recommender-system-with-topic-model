/* After extracting needed fields from training lectures, we also 
 * need to import it into mallet as instances. 
 * We'll use CvsIterator with each line an instance in the file. 
 */
package rs.util.vlc;

import java.util.ArrayList;
import java.util.regex.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.text.Normalizer;

import cc.mallet.pipe.*;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;

public class LectureMalletImporter {
	Pipe pipe;
	public LectureMalletImporter() {
		pipe = buildPipe();
	}
	
	public Pipe buildPipe() {
		// Import document into feature sequence from text
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
		// Pipes: remove html, lowercase, tokenize, remove stopwords, map to features
		pipeList.add( new CharSequenceRemoveHTML() );
		pipeList.add( new CharSequenceLowercase() );
		pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
		pipeList.add( new TokenSequenceRemoveStopwords(new File("stoplists/en.txt.cp"), "UTF-8", false, false, false) );
		pipeList.add( new TokenSequence2FeatureSequence() );
//		pipeList.add( new PrintInputAndTarget() );
		return new SerialPipes(pipeList);
	}
	
	public InstanceList readCsvFile (String dataFile) throws IOException {
		InstanceList instances = new InstanceList(pipe);
		Reader fileReader = new InputStreamReader(new FileInputStream(new File(dataFile)), "UTF-8");
	    instances.addThruPipe( new CsvIterator(fileReader, 
	                               "(\\w+)\\s+(\\w+)\\s+(.*)",
	                               3, 2, 1)  // (data, target, id) field indices
	                         );
		return instances;
	}
	
	public static void main(String[] args) throws IOException {
//		String input = "dataset/lectures_test.csv";
		String output = "dataset/vlc_lectures.all.en.f8.filtered.txt";
		String malletOutput = "dataset/vlc_lectures.all.en.f8.filtered.mallet";
//		ExtractText extractor = new ExtractText(input, output, 0,2,7,8);
//		extractor.doExtraction();
		
		LectureMalletImporter importer = new LectureMalletImporter();
		InstanceList instances = importer.readCsvFile(output);
		instances.save(new File(malletOutput));
		
		InstanceList instances2 = InstanceList.load(new File(malletOutput));
		System.out.println(instances2.get(5235).getName());
		System.out.println(instances2.get(5236).getName());
//		saveCorpus(instances2);
	}
	
	public static void saveCorpus(InstanceList instances) throws IOException {
		FileChannel fc = new FileOutputStream("dataset/vlc_corpus.txt").getChannel();
		Alphabet dataAlphabet = instances.getDataAlphabet();
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<dataAlphabet.size(); i++) {
			sb.append(dataAlphabet.lookupObject(i) + "\n");
		}
		fc.write(ByteBuffer.wrap(sb.toString().getBytes()));
		fc.close();
	}
}
