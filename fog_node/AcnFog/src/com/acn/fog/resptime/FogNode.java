package com.acn.fog.resptime;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import com.acn.fog.other.FogDefinitions;
import com.acn.fog.other.Helper;

/**
 * This class is sent for periodic update to neighbor nodes
 * 
 * @author Vincy Shrine
 *
 */
public class FogNode implements Serializable {

	private static final long serialVersionUID = 1L;
	private static FogNode fogNode;
	private transient List<NeighborNode> neighbors;
	private transient PriorityQueue<NeighborNode> neighborQ;
	private long maxResponseTime;
	
	public static synchronized FogNode getInstance() {
		if (fogNode == null) {
			fogNode = new FogNode();
		}
		return fogNode;
	}
	
	private FogNode(){
		Comparator<NeighborNode> comparator = new Helper.FogCurrentProcessingTimeComparator();
		neighborQ = new PriorityQueue<>(FogDefinitions.PRIORITY_Q_SIZE, comparator);
	}

	private String ipAddress;
	private int tcpPortNo;
	private long currentProcessingTime = 0;
	private int id;
	private transient int udpPortNo;

	public long getCurrentProcessingTime() {
		return currentProcessingTime;
	}

	public void addToCurrentProcessingTime(long incomingPacketProcessingTime) {
		this.currentProcessingTime += incomingPacketProcessingTime;
	}

	public void subtractFromCurrentProcessingTime(long processedPacketProcessingTime) {
		this.currentProcessingTime -= processedPacketProcessingTime;
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getTcpPortNo() {
		return tcpPortNo;
	}

	public void setTcpPortNo(int tcpPortNo) {
		this.tcpPortNo = tcpPortNo;
	}

	public void setUdpPortNo(int portNo) {
		this.udpPortNo = portNo;
	}

	public int getUdpPortNo() {
		return udpPortNo;
	}

	public List<NeighborNode> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(List<NeighborNode> neighbors) {
		this.neighbors = neighbors;
	}

	public PriorityQueue<NeighborNode> getNeighborQ() {
		return neighborQ;
	}

	public void setNeighborQ(PriorityQueue<NeighborNode> neighborQ) {
		this.neighborQ = neighborQ;
	}

	public long getMaxResponseTime() {
		return maxResponseTime;
	}

	public void setMaxResponseTime(long maxResponseTime) {
		this.maxResponseTime = maxResponseTime;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "FogNode [neighborQ=" + neighborQ + ", maxResponseTime=" + maxResponseTime + ", currentProcessingTime="
				+ currentProcessingTime + ", id=" + id + "]";
	}

}