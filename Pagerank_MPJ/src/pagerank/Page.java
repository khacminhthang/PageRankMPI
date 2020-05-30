package pagerank;

import java.io.Serializable;

public class Page implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int node_id;
	private double rank_value;

	public Page() {

	}

	public Page(int iniNodeId, double iniRankValue) {
		this.node_id = iniNodeId;
		this.rank_value = iniRankValue;
	}

	public int getNode_id() {
		return node_id;
	}

	public void setNode_id(int node_id) {
		this.node_id = node_id;
	}

	public double getRank_value() {
		return rank_value;
	}

	public void setRank_value(double rank_value) {
		this.rank_value = rank_value;
	}
}
