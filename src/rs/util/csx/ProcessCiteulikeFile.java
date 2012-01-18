package rs.util.csx;

import java.io.*;
import java.util.*;

public class ProcessCiteulikeFile {

	public static void main(String[] args)
	throws Exception
	{
		String inf = "dataset/csx/current_citeseerx";
		String outf1 = "doi2user.map";
		String outf2 = "user2doi.map";
		int limit=3;
		Hashtable<String, Hashtable<String, String>> user2doimap = new Hashtable<String, Hashtable<String,String>>();
		Hashtable<String, Hashtable<String, String>> doi2usermap = new Hashtable<String, Hashtable<String,String>>();
		Hashtable<String, Integer> usercounts = new Hashtable<String, Integer>();
		Hashtable<String, Integer> doicounts = new Hashtable<String, Integer>();
		
		BufferedReader br = new BufferedReader(new FileReader(inf));
		while(true)
		{
			String line = br.readLine();
			if (line==null)
				break;
			String []parts = line.split("\\|");
			
			String doi = parts[0].trim();
			String userid = parts[2].trim();
			System.out.println("doi="+doi+" userid="+userid);
			
			Hashtable<String, String> dois = user2doimap.get(userid);
			if (dois==null)
				dois = new Hashtable<String, String>();
			dois.put(doi, doi);
			user2doimap.put(userid, dois);
			usercounts.put(userid, dois.size());
			
			
			Hashtable<String, String> users = doi2usermap.get(doi);
			if (users==null)
				users = new Hashtable<String, String>();
			users.put(userid, userid);
			doi2usermap.put(doi, users);
			doicounts.put(doi, users.size());
			
		}
		
		br.close();
		
		System.out.println("Size of doi2usermap:"+doi2usermap.size());
		System.out.println("Size of user2doimap:"+user2doimap.size());
		
		
		PrintWriter pw = new PrintWriter(new FileWriter(outf1), true);
		
		for (Enumeration<String> dx=doi2usermap.keys(); dx.hasMoreElements();)
		{
			String doi = dx.nextElement();
			int count = doicounts.get(doi);
			if (count<limit) continue;
			
			Hashtable<String, String> users = doi2usermap.get(doi);
			String usrstr="";
			for (Enumeration<String> ux=users.keys(); ux.hasMoreElements();)
			{
				String userid = ux.nextElement();
				count = usercounts.get(userid);
				if (count<limit) continue;
				usrstr+=" "+userid;
			}
			pw.println(doi+" "+count+" "+usrstr.trim());
		}
		pw.close();
		
		pw = new PrintWriter(new FileWriter(outf2), true);
		
		for (Enumeration<String> ux=user2doimap.keys(); ux.hasMoreElements();)
		{
			String user = ux.nextElement();
			int count = usercounts.get(user);
			if (count<limit) continue;
			
			Hashtable<String, String> dois = user2doimap.get(user);
			String docstr="";
			for (Enumeration<String> dx=dois.keys(); dx.hasMoreElements();)
			{
				String doi = dx.nextElement();
				count = doicounts.get(doi);
				if (count<limit) continue;
				docstr+=" "+doi;
			}
			pw.println(user+" "+count+" "+docstr.trim());
		}
		pw.close();
		
		
	}
}
