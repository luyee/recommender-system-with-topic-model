/* To extract needed fields from vlc lecture meta data into mallet 
 * csv format. This csv format is like "id label data"
 */
package rs.util.vlc;

import java.io.*;
import java.text.Normalizer;
import java.util.*;

import rs.util.StringRemoveStopWords;

import au.com.bytecode.opencsv.*;

public class ExtractText {
	public static final int NO_FILTER_INDEX = -1;
	public static final String NO_FILTER_VALUE = null; 
	public static final char DEFAULT_SEPARATOR = ' ';
	public static final int DOC_SIZE_THRESHODL = 2;	// In case we need to filter out too short documents.
	
	final String pattern = "\\p{L}[\\p{L}\\p{P}]+\\p{L}";
	StringRemoveStopWords stopper;
		
	public void setStopper(StringRemoveStopWords stopper) {
		this.stopper = stopper;
	}

	String inputFileName;	// Input file name
	String outputFileName;	// Output file name
	int[] fieldIdx;			// Fileds that need to be extracted, 
							// notice there could be order in the sequence
	char separator;			// Separator when output text, by default it's white space
	int filterIdx;			// To filter one value in this field index.
	String filterStr;		// only when the filter index value equals 
							// this string will we take care, otherwise just ignore that line. 
	String[] fields;

	public ExtractText(String input, String output, char separator, int... fields) {
		this(input, output, separator, NO_FILTER_INDEX, NO_FILTER_VALUE, fields);
	}
	
	public ExtractText(String input, String output, int... fields) {
		this(input, output, DEFAULT_SEPARATOR, fields);	// Use whitespace as a default separator
	}
	
	public ExtractText(String input, String output, 
			int filterIdx, String filterVal, int... fields) {
		this(input, output, DEFAULT_SEPARATOR, filterIdx, filterVal, fields);
	}
	
	public ExtractText(String input, String output, char separator, 
				int filterIdx, String filterVal, int... fields) {
		this.inputFileName = input;
		this.outputFileName = output;
		this.separator = separator;
		this.filterIdx = filterIdx;
		this.filterStr = filterVal;
		fieldIdx = (int[]) fields.clone();
	}

	/*
	 * Check if the fields pass filter in specified field.
	 */
	private boolean isFieldValid(String[] fields) {
		if ( this.filterIdx != NO_FILTER_INDEX &&
				this.filterIdx < fields.length &&
				this.filterStr != NO_FILTER_VALUE ) 
		{
			if (!fields[filterIdx].trim().equalsIgnoreCase(this.filterStr)) 
				return false;
		}
		return true;
	}
	
	/*
	 * Check if the string words longer than threshold
	 */
	private boolean qualifyLength(String[] fields) {
		int total = 0;
		for(int i=0; i<fields.length; i++) {
//			String[] tmp = fields[i].split("\\W");
//			total += tmp.length;
			if (i>1) {
				String[] tmp = fields[i].split("\\W+");
				for (int j=0; j<tmp.length; j++) {
					if(!tmp[j].matches(pattern)) continue;
					if(stopper.isStopword(tmp[j])) continue;
					total++;					
				}
			}
		}
		if(total > DOC_SIZE_THRESHODL) 
			return true;
		return false;
	}
	
	public boolean doExtraction() {
		CSVReader reader = null;
		CSVWriter writer = null;
		try {
			reader = new CSVReader(new InputStreamReader(
					new FileInputStream(inputFileName), "UTF-8"), ',', '\'');
			writer = new CSVWriter(new FileWriter(outputFileName), 
					' ', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER);
			String[] nextLine;
			int lineno=0, newlineno=0;
			while ((nextLine = reader.readNext()) != null) {
				lineno++;				
				if(!isFieldValid(nextLine)) continue;
				
				String[] newLine = new String[fieldIdx.length];
				
				for (int i=0; i < fieldIdx.length; i++) {
					if (fieldIdx[i] >= nextLine.length) return false;
					
					if( "null".equalsIgnoreCase(nextLine[fieldIdx[i]].trim())) {
						newLine[i] = "";
					} else {
						newLine[i] = nextLine[fieldIdx[i]].trim();
					}
				}
				if (!qualifyLength(newLine)) 
					continue;
				System.out.println(newLine[0]);
				writer.writeNext(newLine);
				if (newLine != null)
					newlineno++;
			}
			System.out.println("Extracted lines: " + lineno + ", output lines: " + newlineno);
			reader.close();
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (reader != null) reader.close();
				if (writer != null) writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public static void main(String[] args) {
		String input = "dataset/lectures_train.csv";
//		String input = "dataset/test.txt";
		String output = "dataset/vlc/vlc_train.title.en.f2.txt";
//		String output = "dataset/vlc_lectures_train.txt";
//		ExtractText extractor = new ExtractText(input, output, 2, "en", 0,2,7,8);
//		ExtractText extractor = new ExtractText(input, output, 2, "en", 0,2,6,7);
		ExtractText extractor = new ExtractText(input, output, 2, "en", 0,2,7);
		StringRemoveStopWords stopper = new StringRemoveStopWords(new File("stoplists/en.txt.cp"), "UTF-8");
		extractor.setStopper(stopper);
		
//		ExtractText extractor = new ExtractText(input, output, 0,2,6,7);
		if (extractor.doExtraction()) {
			System.out.println("Extracted successfully.");
		} else {
			System.out.println("Extraction failed.");
		}
	}

}
