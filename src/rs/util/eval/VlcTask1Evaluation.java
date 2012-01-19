/**
 * @author Sujatha 01/19/2012
 */
package rs.util.eval;

import java.io.*;
import java.util.*;

import rs.types.CompObj;

public class VlcTask1Evaluation {


	public static void main(String[] args)
	throws Exception
	{
		String predpairsf = args[0];
		
		int topk=new Integer(args[1]);
		
		Hashtable<String, Hashtable<String, String>> correct = new Hashtable<String, Hashtable<String,String>>();
		Hashtable<String, Vector<CompObj>> preds = new Hashtable<String, Vector<CompObj>>();
		
		BufferedReader br = new BufferedReader(new FileReader(predpairsf));
		while (true)
		{
			String line = br.readLine();
			if (line==null)
				break;
			String []parts = line.split("[ ]+|\t");
			String id1 = parts[0].trim();
			String id2 = parts[1].trim();
			
			Integer corr = new Integer(parts[2].trim());
			Integer pred = new Integer(parts[3].trim());
			Double negprob = new Double(parts[4].trim());
			Double posprob = new Double(parts[5].trim());
			
			if (corr==1)
			{
				Hashtable<String, String> corrforthisid = correct.get(id1);
				if (corrforthisid==null)
					corrforthisid = new Hashtable<String, String>();
				corrforthisid.put(id2, id2);
				correct.put(id1, corrforthisid);
			}
			
			if (pred==1)
			{
				Vector<CompObj> predsforthisid = preds.get(id1);
				if (predsforthisid==null)
					predsforthisid = new Vector<CompObj>();
				CompObj cobj = new CompObj(id2, posprob);
				predsforthisid.add(cobj);
				preds.put(id1, predsforthisid);
			}
			
		}
		br.close();
		System.out.println("correct.size:"+correct.size());
		System.out.println("preds.size:"+preds.size());
		
		int numegs = 0;
		double aggprec=0;
		double aggrec=0;
		
		for (Enumeration<String> idx = preds.keys(); idx.hasMoreElements();)
		{
			
			String testid = idx.nextElement();
			Vector<CompObj> predsforid = preds.get(testid);
			Collections.sort(predsforid);
			Collections.reverse(predsforid);
		
			Hashtable<String, String> corrforid = correct.get(testid);
			if (corrforid==null || corrforid.size()==0)
				continue;
			
			
			double totcorr=0;
			int limit = Math.min(predsforid.size(), topk);
			
			if (limit==0) continue;
			
			numegs++;
			for (int px=0; px<limit; px++)
			{
				CompObj predobj = predsforid.get(px);
				if (corrforid.get(predobj._id)!=null)
				{
					totcorr++;
				}
			}
			
			aggprec += totcorr/limit;
			aggrec += totcorr/corrforid.size();			
		}
		
		System.out.println("Avg Precision:"+aggprec/numegs);
		System.out.println("Avg Recall:"+aggrec/numegs);
	}
	
}