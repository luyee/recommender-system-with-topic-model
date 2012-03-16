package rs.types;

import gnu.trove.map.hash.*;

public class PairedInfo {
	TIntDoubleHashMap pairedVideos;
	int id;			// This is just the index.
	
	public PairedInfo(int id) {
		this.id = id;
		this.pairedVideos = new TIntDoubleHashMap();
	}
	
	public boolean isEmpty() {
		if (pairedVideos.size() == 0) 
			return true;
		return false;
	}
	
	public void add(int pairedId, double sim) {
		if (!pairedVideos.contains(pairedId)) {
			pairedVideos.put(pairedId, sim);
		}
	}
	
	/**
	 * Value less than 0 means it doesn't exist.
	 * @param pid
	 * @return
	 */
	public double getSim(int pid) {
		if(pairedVideos.contains(pid)) {
			return pairedVideos.get(pid);
		} else {
			return 0;
		}
	}
	
	public TIntDoubleHashMap getPairedVideos() {
		return this.pairedVideos;
	}

	public int getLength() {
		return pairedVideos.size();
	}
	
	public int[] getPairedIdsArray() {
		return pairedVideos.keys();
	}
	
	public double[] getPairedSimArray() {
		return pairedVideos.values();
	}
}
