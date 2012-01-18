package rs.util.csx;

import java.io.*;
import java.util.*;

public class GeneratePairs {
	public static void main(String[] args)
	throws Exception
	{
		String inf =  "user2doi.map";
		String outf = "pair.counts";
		int limit=3; 
		
		Hashtable<String, Integer> paircounts = new Hashtable<String, Integer>();
		
		BufferedReader br = new BufferedReader(new FileReader(inf));
		while(true)
		{
			String line = br.readLine();
			if (line==null)
				break;
			String []parts = line.split(" ");			
			//String userid = parts[0].trim();
			String []dois = new String[parts.length-2];
			for (int px=2; px<parts.length; px++)
				dois[px-2] = parts[px].trim();
			for (int i=0; i<dois.length-1; i++)
			{
				for (int j=i+1; j<dois.length; j++)
				{
					String lkup = dois[i]+" "+dois[j];
					Integer count = paircounts.get(lkup);
					if (count==null)
					{
						lkup = dois[j]+" "+dois[i];
						count = paircounts.get(lkup);
					}
					if (count==null)
						count=0;
					count++;
					paircounts.put(lkup, count);					
				}
			}			
		}		
		br.close();
		
		PrintWriter pw = new PrintWriter(new FileWriter(outf), true);
		
		for (Enumeration<String> px=paircounts.keys(); px.hasMoreElements();)
		{
			String doipair = px.nextElement();
			int count = paircounts.get(doipair);
			if (count<limit)
				continue;
			pw.println(doipair+" "+count);
		}		
		pw.close();
	}

}
