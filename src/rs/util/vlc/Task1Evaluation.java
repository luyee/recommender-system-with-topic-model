/**
 * This class comes from Vlc challenge. 
 */
package rs.util.vlc;

import java.io.*;
import java.util.Arrays;

import org.tunedit.core.*;
import org.tunedit.core.exception.*;

public class Task1Evaluation extends EvaluationProcedure {
		public int z_max;
		public Task1Evaluation(int z) {
			this.z_max = z;
		}
	
        public Double[] run(ResourceName userLabelsName, ResourceName targetLabelsName, ResourceLoader loader)
        	throws TunedTesterException, AlgorithmErrorException
        {
        		boolean final_eval = true;
        		// for preliminary put false, for final put true
        	
                /* Open the resources */
                BufferedReader userLabels = new BufferedReader(new InputStreamReader( loader.open(userLabelsName) ));
                BufferedReader targetLabels = new BufferedReader(new InputStreamReader( loader.open(targetLabelsName) ));
                int lineCount = 0;
                try {
                	
                	String targetLine, userLine, targetLecture, userLecture;
                	int targetStart, userStart, lectureCount = 0;
                	int isInLeaderBoard = 0;
                	double tempRecall = 0., totalRecall = 0.;
                	int max_lecture_index = 15002;
                	int[] s_duplicates = new int[max_lecture_index], r_top_z = new int[max_lecture_index];
                	Arrays.fill(r_top_z, 0);
                	Arrays.fill(s_duplicates, 0);
                	
                	while ((targetLine = targetLabels.readLine()) != null) {
                		lineCount++;
                		if ((userLine = userLabels.readLine()) == null) throw new AlgorithmErrorException("Solution file has an incorrect format at line: " + lineCount +" ! Solution file does not have appropriate number of rows !");		// user line end!! must throw exception
                		
                		userLine = userLine.replace(" ", ""); 
                    	targetStart = targetLine.indexOf(':');
                    	userStart = userLine.indexOf(':');
						targetLecture = targetLine.substring(0, targetStart);
						userLecture = userLine.substring(0, userStart);
                    	
                    	if (targetLecture.compareTo(userLecture) != 0) throw new AlgorithmErrorException("Solution file has an incorrect format at line: " + lineCount +"! Recommendation list for lecture: " + targetLecture + " was expecting !");		// different lecture
                    	
                    	int targetStart2 = targetLine.indexOf(':',targetStart+1);
                    	String LeaderBoardStr = targetLine.substring(targetStart+1, targetStart2) ;
                    	isInLeaderBoard = Integer.valueOf( LeaderBoardStr );
                    	
                    	
                    	if (targetLine.length() - 1 == targetStart2) continue;					// skip empty lines
                    	
                    	if ( (isInLeaderBoard == 1) || (final_eval) ) { 
                    		++lectureCount;
                    	}
                    		                 
                    	String[] targetSplit = targetLine.substring(targetStart2 + 1, targetLine.length()).split(",");
                    	String[] userSplit = userLine.substring(userStart + 1, userLine.length()).split(",");

                    	int[] user = new int[z_max];
                    	int[] target = new int[targetSplit.length];
                    	int[] targetCVS = new int[targetSplit.length];

                    	String[] temp;
                    	for (int i = 0 ; i < targetSplit.length ; ++i){
                    		temp = targetSplit[i].split("\\|");
                    		target[i] = Integer.valueOf(temp[0]);
                    		targetCVS[i] = Integer.valueOf(temp[1]);
                    	}
                    	
                    	for (int i = 0 ; i < z_max ; ++i)
                    		if (i < userSplit.length)
                    			user[i] = Integer.valueOf(userSplit[i]);
                    		else
                    			user[i] = -1;
                    	
                    	int z_size = z_max / 5;
                    	//NINOTOVLJEV DIO KODA!!!
                    	int i = 0, m = target.length;
                    	double TP;
                    	tempRecall = 0;
                    	for (int z = 5 ; z <= z_max ; z += 5) {
                    		
                    		while (true) {
                    			if (i < Math.min(m,z)) {
                    				r_top_z[target[i++]] = 1;
                    			} else {
                    				if (i < targetCVS.length && targetCVS[i] == targetCVS[i-1]) {
                    					r_top_z[target[i++]] = 1;
                    				} else {
                    					break;
                    				}
                    			}
                    		}
                    		TP = 0;
                    		for (int j = 0 ; j < z ; ++j){
                    			if (user[j] != -1) {
                    				if (s_duplicates[user[j]] == 0) {
                    					s_duplicates[user[j]] = 1;
                    					if (r_top_z[user[j]] == 1) {
                    						++TP;
                    					}
                    				}
                    			} else {
                    				break;
                    			}
                    		}
                    		tempRecall += TP / Math.min(m,z);
                    		for (int j = 0 ; j < z ; ++j){
                    			if (user[j] != -1) {
                    				s_duplicates[user[j]]=0;
                    			}
                    		}
                    	}
                    	double tempRecallNorm = tempRecall / z_size;
                    	if ( (isInLeaderBoard == 1) || (final_eval) ) {
                    		totalRecall += tempRecallNorm;
                    	}
                    	for (int j = 0 ; j < i ; ++j){
                    		r_top_z[target[j]] = 0;
                    	}
                	}
                	return new Double[] {totalRecall /= lectureCount};
                }catch (IOException e) {
                        throw new AlgorithmErrorException("Solution file has an incorrect format at line: "+lineCount+" !");
                }catch (NumberFormatException e){
                		throw new AlgorithmErrorException("Solution file has an incorrect format at line: "+lineCount+" ! Number was expected ! ");
                }catch (NullPointerException e) {
                		throw new AlgorithmErrorException("Solution file has an incorrect format at line: "+lineCount+" !");
                }catch ( ArrayIndexOutOfBoundsException e){
                	    throw new AlgorithmErrorException("Solution file has an incorrect format at line: "+lineCount+" ! Number out of lecture bounds !");
                }
        }
}
