/* This class does some simple stop word removing work. It's modified 
 * based on mallet pipe  TokenSequenceRemoveStopwords 
 */
package rs.util;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.*;


public class StringRemoveStopWords {
	String pattern = "\\p{L}[\\p{L}\\p{P}]+\\p{L}";
	HashSet<String> stoplist = null;
	
	public StringRemoveStopWords (File stoplistFile, String encoding) {
		stoplist = new HashSet<String>();
		addStopWords (fileToStringArray(stoplistFile, encoding));
	}
	
	public HashSet<String> getStoplist() {
		return this.stoplist;
	}
	
	public StringRemoveStopWords addStopWords (String[] words)
	{
		for (int i = 0; i < words.length; i++)
			stoplist.add (words[i]);
		return this;
	}
	
	private String[] fileToStringArray (File f, String encoding)
	{
		ArrayList<String> wordarray = new ArrayList<String>();

		try {

			BufferedReader input = null;
			if (encoding == null) {
				input = new BufferedReader (new FileReader (f));
			}
			else {
				input = new BufferedReader( new InputStreamReader( new FileInputStream(f), encoding ));
			}
			String line;

			while (( line = input.readLine()) != null) {
				String[] words = line.split ("\\s+");
				for (int i = 0; i < words.length; i++)
					wordarray.add (words[i]);
			}

		} catch (IOException e) {
			throw new IllegalArgumentException("Trouble reading file "+f);
		}
		return (String[]) wordarray.toArray(new String[]{});
	}
	
	public boolean isStopword(String word) {
		if (stoplist.contains(word.toLowerCase()))
			return true;
		return false;
	}
	
	public static HashSet<String> buildStoplist (String stoplistfile) {
		StringRemoveStopWords s = new StringRemoveStopWords(new File(stoplistfile), "UTF-8");
		return s.getStoplist();
	}
}
