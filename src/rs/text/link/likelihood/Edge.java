package rs.text.link.likelihood;

public class Edge {
	public int node1;
	public int node2;
	public double weight;
	
	public Edge(int n1, int n2, double w) {
		this.node1 = n1;
		this.node2 = n2;
		this.weight = w;
	}
}
