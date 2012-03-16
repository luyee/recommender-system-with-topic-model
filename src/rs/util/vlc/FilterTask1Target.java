/**
 * This class will filter out pairs from task 1 target with the same 
 * threshold, we do it with number threshold, instead of percentage.
 * @author Haibin
 *
 */
package rs.util.vlc;

import java.io.*;

import org.tunedit.core.exception.AlgorithmErrorException;

public class FilterTask1Target {
	public static void filterTarget(int threshold, String srcFile, String dstFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(srcFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(dstFile));
		String srcLine, dstLine;
		while ((srcLine = reader.readLine()) != null) {
			srcLine = srcLine.replace(" ", "");
			int targetStart = srcLine.indexOf(':');
			int targetStart2 = srcLine.indexOf(':', targetStart+1);
			String targetVideo = srcLine.substring(0, targetStart);
			String leaderBoardStr = srcLine.substring(targetStart+1, targetStart2);
			if (srcLine.length() - 1 == targetStart2) continue;
			String[] srcSplit = srcLine.substring(targetStart2 + 1, srcLine.length()).split(",");
			StringBuffer buffer = new StringBuffer();
			for(int i=0; i<srcSplit.length; i++) {
				String[] temp = srcSplit[i].split("\\|");
				int freq = Integer.valueOf(temp[1]);
				if (freq < threshold) continue;
				buffer.append(temp[0]+"|"+temp[1]+",");
			}
			if (buffer.length() == 0) continue;
			else buffer.deleteCharAt(buffer.length() - 1);
			writer.write(targetVideo + ":" + leaderBoardStr + ":" + buffer.toString());
			writer.newLine();
		}
		reader.close();
		writer.flush();
		writer.close();
	}
	
	public static void main(String[] args) {
		try {
			filterTarget(5, "dataset/vlc/task1_target.en.f8.txt", "dataset/vlc/task1_target.en.f8.n5.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
