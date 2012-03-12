
/**
 * In this class does some preprocessing on task1 query. We want to remove 
 * those videos in query which do not have enough co-viewed videos, and those  
 * not in English.
 * 
 * Currently I have 2 ideas of preprocessing: 
 * 1. Remove unwanted queries from videos and query.
 * 2. Generate a cross validation dataset ourselves.
 * @author Haibin
 *
 */

package rs.util.vlc;

import java.io.*;
import java.util.regex.*;

import gnu.trove.map.hash.*;


public class PreProcessTask1Query {
	
	/*
	 * Replace punctuation with spaces, and remove words shorter than 2 letters. 
	 */
	static public void filterDocuments(String source, String dest) 
		throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(source));
		BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
		String line;
		while ( (line = reader.readLine()) != null) {
			line = line.trim();
			String[] fields = line.split("\\s+", 3);
			if(fields.length < 3) continue;
			String text = PreProcessTask1Query.removeMarks(fields[2]);
			text = PreProcessTask1Query.removeShortwords(text);
			writer.write(fields[0] + " " + fields[1] + " " + text);
			writer.newLine();
		}
		reader.close();
		writer.flush();
		writer.close();
	}
	
	/**
	 * Replace punctuations in string with spaces.
	 * @param source
	 * @return
	 */
	public static String removeMarks(String source) {
		String regex = "\\p{Punct}";
		return removeExtraSpaces( source.replaceAll(regex, " ") );
	}
	
	/**
	 * Remove words shorter than 3 letters.
	 * @param source
	 * @return
	 */
	public static String removeShortwords(String source) {
		String regex = "\\b\\w{1,2}\\b";
		return removeExtraSpaces( source.replaceAll(regex, " ") );
	}
	
	public static String removeExtraSpaces(String source) {
		return source.replaceAll("\\s+", " ");
	}
	
	/**
	 * Read from an id file and put it into a hashmap. 
	 * @param idFile one id per line
	 * @return TObjectIntHashMap
	 * @throws IOException
	 */
	static public TObjectIntHashMap<String> readIdHash(String idFile) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(idFile)));
		TObjectIntHashMap<String> idHash = new TObjectIntHashMap<String>();
		String line; 
		while( (line = reader.readLine()) != null) {
			idHash.put(line.trim(), 1);
		}
		reader.close();
		return idHash;
	}
	
	/**
	 * Filter out unwanted query video id. 
	 * @param idFile stores only needed video ids, only ids in the file will be remained in query
	 * @param queryFile
	 * @param newQueryFile
	 * @throws IOException 
	 */
	static public void filterQuery(String idFile, String queryFile, String newQueryFile) 
		throws IOException {
		BufferedReader reader;
		String line;
		TObjectIntHashMap<String> idHash = readIdHash(idFile);
		reader = new BufferedReader(new InputStreamReader(
									new FileInputStream(queryFile)));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
									new FileOutputStream(newQueryFile)));
		while( (line = reader.readLine()) != null) {
			if(idHash.containsKey(line.trim())) {
				writer.write(line.trim());
				writer.newLine();
			}
		}
		reader.close();
		writer.flush();
		writer.close();		
	}
	
	static public void filterSolution(String idFile, String labelFile, String newLabelFile) throws IOException {
		TObjectIntHashMap<String> idHash = readIdHash(idFile);
		BufferedReader reader = new BufferedReader(new InputStreamReader(
									new FileInputStream(labelFile)));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
									new FileOutputStream(newLabelFile)));
		
		String line;
		while( (line = reader.readLine()) != null) {
			line = line.trim();
			int idIdx = line.indexOf(":");
			String id = line.substring(0, idIdx);
			if(idHash.containsKey(id)) {
				writer.write(line);
				writer.newLine();
			}
		}
		reader.close();
		writer.flush();
		writer.close();
	}
	
	static public void main(String[] args) throws IOException {
		PreProcessTask1Query.filterQuery("dataset/vlc/ids.en.title.f2.train.txt", 
					"dataset/vlc/task1_query.csv", "dataset/vlc/task1_query.en.title.f2.txt");
		PreProcessTask1Query.filterSolution("dataset/vlc/ids.en.title.f2.train.txt", "dataset/vlc/task1_target.txt",
				"dataset/vlc/task1_target.en.title.f2.txt");
//		PreProcessTask1Query.filterDocuments(
//				"dataset/vlc_lectures.all.en.f8.txt", 
//				"dataset/vlc_lectures.all.en.f8.filtered.txt");
	}
}
