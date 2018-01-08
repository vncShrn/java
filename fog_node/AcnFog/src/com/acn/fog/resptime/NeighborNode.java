package com.acn.fog.resptime;

/**
 * Neighbour Class used for priorityQ to store and update info about neighbor
 * current processing time
 * 
 * @author Vincy Shrine
 *
 */
public class NeighborNode {

	private String ipAddress;
	private int tcpPortNo;
	private int id;
	private long currentProcessingTime;

	public NeighborNode(String ipAddress, int tcpPortNo, int id) {
		this.ipAddress = ipAddress;
		this.tcpPortNo = tcpPortNo;
		this.id = id;
	}

	public int getTcpPortNo() {
		return tcpPortNo;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public Integer getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Long getCurrentProcessingTime() {
		return currentProcessingTime;
	}

	public void setCurrentProcessingTime(long currentProcessingTime) {
		this.currentProcessingTime = currentProcessingTime;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + ((ipAddress == null) ? 0 : ipAddress.hashCode());
		result = prime * result + tcpPortNo;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NeighborNode other = (NeighborNode) obj;
		if (id != other.id)
			return false;
		if (ipAddress == null) {
			if (other.ipAddress != null)
				return false;
		} else if (!ipAddress.equals(other.ipAddress))
			return false;
		if (tcpPortNo != other.tcpPortNo)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return id + ":" + currentProcessingTime;
	}
}
