package rs.util;

import java.text.Normalizer;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.MalformedInputException;

/** 
 * This class tries to convert some unicode characters that cannot be correctly 
 * displayed in unix into ascii letters 
 */

public class TextNormalizer {
	public static final int BSIZE = 1024;
	public static void normalize(String inputFile, String outputFile) 
	throws IOException {
		Charset charset = Charset.forName("UTF-8");
		CharsetDecoder decoder = charset.newDecoder();
		
		FileChannel reader = new FileInputStream(inputFile).getChannel();
		FileChannel writer = new FileOutputStream(outputFile).getChannel();
		ByteBuffer buff = ByteBuffer.allocate(BSIZE);
		ByteBuffer wbuff = ByteBuffer.allocate(BSIZE);
		
		while(reader.read(buff) != -1) {
			buff.flip();
			StringBuilder sb;
			String s1;
			try {
				sb = new StringBuilder(decoder.decode(buff));
			} catch(MalformedInputException e) {
				e.printStackTrace();
				sb = new StringBuilder(buff.asCharBuffer());
			}
			s1 = Normalizer.normalize(sb.toString(), Normalizer.Form.NFKD);
			String regex = "[\\p{InLatin-1Supplement}]+";
			String s2 = new String(s1.replaceAll(regex, " ").getBytes("ascii"),"ascii");
			s2 = s2.replaceAll("[?]+", " ");
			wbuff.put(s2.getBytes());
			wbuff.flip();
			writer.write(wbuff);
			wbuff.clear();
			buff.clear();
		}
		reader.close();
		writer.close();
	}
	

	public static void normalize2(String inputFile, String outputFile) 
	throws IOException {		
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		String line;
		while ( (line = reader.readLine()) != null) {
			String s1 = Normalizer.normalize(line, Normalizer.Form.NFKD);
			String regex = "[\\p{InLatin-1Supplement}]+";
			String s2 = new String(s1.replaceAll(regex, " ").getBytes("ascii"),"ascii");
			s2 = s2.replaceAll("[?]+", " ");
			writer.write(s2);
			writer.newLine();
		}
		writer.flush();
		writer.close();
		reader.close();
	}

	public static void main(String[] args) throws IOException {
		normalize2("dataset/vlc/vlc_train.title.en.f2.txt", "dataset/vlc/vlc_train.title.en.f2.txt.2");
	}
}
