/**
 * @author Sujatha 
 * 01/19/2012
 */
package rs.types;

public class CompObj implements Comparable{

	public String _id;
	public double _val;
	public CompObj(String id, double val) {
		// TODO Auto-generated constructor stub
		_id = id;
		_val = val;		
	}
	
	public int compareTo(Object o)
	{
		CompObj other = (CompObj)o;
		if (this._val > other._val)
			return 1;
		else if (this._val < other._val)
			return -1;
		else
			return 0;
	}	
}
