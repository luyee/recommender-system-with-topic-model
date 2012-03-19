package rs.types;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class PairData {
	public static final String BY = "x";
	public InstanceList documents;
	public TObjectIntHashMap<String> idHash;
	public TObjectIntHashMap<String> pairIdHash;
	public ArrayList<PairedInfo> links;
	public int numOfLinks;

	public PairData() {
		super();
	}

	public void addPair(int doc1, int doc2, double sim) {
		PairedInfo p = links.get(doc1);
		if (p == null) {
			p = new PairedInfo(doc1);
		}
		p.add(doc2, sim);
	}
	
	

	public boolean containsPair(int v1, int v2) {
		if (pairIdHash.containsKey(pairIdString(v1, v2)) ||
				pairIdHash.containsKey(pairIdString(v2, v1)) ) 
			return true;
		else 
			return false;
	}

	public ArrayList<PairedInfo> getLinks() {
		return links;
	}

	public int getNumOfLinks() {
		return this.numOfLinks;
	}

	/**
	 * Return the sim value of a specified pair. 
	 * 
	 * @param v1
	 * @param v2
	 * @return Less than 0 means this pair does not exist.
	 */
	public double getPairedSim(int v1, int v2) {
		if (links.get(v1) != null)
			return links.get(v1).getSim(v2);
		return -1;
	}

	/**
	 * Init idHahs according to documents, here it is an instance list.
	 */
	protected void initIdHash() {
		if (documents == null) {
			System.err.println("Document instance should be initialized first.");
			return;
		}
		if(idHash == null) {
			idHash = new TObjectIntHashMap<String>();
		}
		for(int i=0; i<documents.size(); i++) {
			Instance doc = documents.get(i);
			String vId = (String) doc.getName();
			idHash.put(vId, i);
		}
	}

	private String pairIdString(int v1, int v2) {
		return Integer.toString(v1) + BY + Integer.toString(v2);
	}

	/**
	 * Import links data into system. Here link means co-viewed pair info, 
	 * including both id and frequency.
	 * @param linkFile This is the file with normalized similarity values.
	 * @return 
	 * @throws IOException 
	 */
	public double[] readLinksIntoMemory(String linkFile) throws IOException {
		if (links == null) {
			links = new ArrayList<PairedInfo>(documents.size());
			for(int i=0; i<documents.size(); i++) {
				links.add(new PairedInfo(i));
			}
		}
		pairIdHash = new TObjectIntHashMap<String>();
		TDoubleArrayList simArray = new TDoubleArrayList();
		
		BufferedReader reader = new BufferedReader(
				new FileReader(linkFile));
		String s;
		
		while( (s=reader.readLine()) != null) {
			if (s.trim().startsWith("#")) continue;
			String[] fields = s.split("\\s*,\\s*");
			if (fields.length != 3) continue;
			int doc1, doc2;
			double sim;
			
			if ( !(idHash.containsKey(fields[0].trim())
					&& idHash.containsKey(fields[1].trim())) ) {
//				System.out.println(fields[0] + "," + fields[1]);
				continue;
			}
			doc1 = idHash.get(fields[0].trim());
			doc2 = idHash.get(fields[1].trim());
			sim = Double.parseDouble(fields[2]);
			
			if( !this.containsPair(doc1, doc2)) {
				pairIdHash.put(pairIdString(doc1, doc2), numOfLinks);
				simArray.add(sim);
				numOfLinks++;
			} else {
				System.err.println("Duplicated pair:" + fields[0] + ", " + fields[1] + fields[2]);
			}
			
			this.addPair(doc1, doc2, sim);
			this.addPair(doc2, doc1, sim);
		}
		reader.close();
		return simArray.toArray();
	}

	public void initFromFile(String trainMalletFile, String linkFile) throws IOException {
		documents = InstanceList.load(new File(trainMalletFile));
		initIdHash();
		readLinksIntoMemory(linkFile);
	}
}