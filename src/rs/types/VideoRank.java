package rs.types;

public class VideoRank implements Comparable {
	public double sim;
	public String id;
	public VideoRank ( double sim, String id) { 
		this.sim = sim; 
		this.id = id; 
	}
	/**
	 * Sort in reverse order
	 * @param o2
	 * @return
	 */
	public final int compareTo (Object o2) {
		if (sim > ((VideoRank)o2).sim)
			return -1;
		else if (sim == ((VideoRank)o2).sim)
			return 0;
		else return 1;
	}
}