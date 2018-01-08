package com.aos.me.packets;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class NeighborsPacket implements Packet {

	public NeighborsPacket(Node node) {
		super();
		this.node = node;
	}

	private static final long serialVersionUID = 1L;

	private Node node;

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	@Override
	public String toString() {
		return "NEIGHBORS [" + node + "]";
	}

}
