package rs.types;
/**
 * Different from the above class that we are more interested in the 
 * indices here than doc id.
 * @author Haibin
 *
 */
public class VideoRankIndices implements Comparable {
	public double sim;
	public int index;
	public VideoRankIndices (double sim, int index) { this.sim = sim; this.index = index; }
	/**
	 * Sort in reverse order
	 * @param o2
	 * @return
	 */
	public final int compareTo (Object o2) {
		if (sim > ((VideoRankIndices)o2).sim)
			return -1;
		else if (sim == ((VideoRankIndices)o2).sim)
			return 0;
		else return 1;
	}
}