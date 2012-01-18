package rs.util.csx;

import java.io.*;

import java.sql.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class GetCSXAbstracts {

	private String dburl;
	private String uname;
	private String upwd;
	private Connection con;

	public GetCSXAbstracts(String _url, String _uname, String _pass)
	{
		dburl = _url;
		uname = _uname;
		upwd = _pass;
		con = null;
	}

	public Statement openConnection()
	{
		Statement s = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");

			Connection con = DriverManager.getConnection(
					dburl, uname, upwd);			
			if (con == null){
				System.err.println("Could not open DB connection");
				System.exit(1);
			}

			s = con.createStatement();			
			if (s == null){
				System.err.println("Could not create statement");
				System.exit(1);				
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return s;
	}

	public void closeConnection()
	{
		try {
			if (con != null)
				con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public void getAllAbstracts(String doisf, String outFile)
	{
		try {
			PrintWriter pw = 
				new PrintWriter(new FileOutputStream(outFile), true);
			Statement s = openConnection();
			BufferedReader br = new BufferedReader(new FileReader(doisf));
			while (true)
			{
				String line = br.readLine();
				if (line==null)
					break;
				String doi=line.trim();
				String str = getTitleAbstract(s, doi);
				if (str!=null && !str.trim().equals(""))
					pw.println(doi+" "+str);
			}
			br.close();
			closeConnection();
			pw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getTitleAbstract(Statement s, String doi) 
	{
		String toret="";
		try {

			ResultSet rs = s.executeQuery(
			"select title, abstract from citeseerx.papers where id='"+doi+"'");

			if (rs.next())
			{
				toret=rs.getString(1)+" "+rs.getString(2);
			}

			rs.close();
			
		} catch (SQLException e) {
			//e.printStackTrace();
			return "";
		}
		return toret;
	}

	public static void main(String[] args)
	{
		String dbConnectUrl="jdbc:mysql://brick3.ist.psu.edu:3306";
		String dbUser="csx-read";
		String dbPass="csx-read";
		String outFile = "csx.auths";
		String doisf = "dataset/csx/dois.list"; 
		String outf = "dataset/csx/dois.abstracts";
		new GetCSXAbstracts(dbConnectUrl, dbUser, dbPass).getAllAbstracts(doisf, outf);
	}


}//end class