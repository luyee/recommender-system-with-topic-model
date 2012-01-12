package rs.util;

public class Sampling {
	static final MTRandom random = new MTRandom();
	
	public static int nextInt(int seed) {
		return random.nextInt(seed);
	}
	
	public static int multinomialSampling(double[] weights) {
		return 0;
	}
}
