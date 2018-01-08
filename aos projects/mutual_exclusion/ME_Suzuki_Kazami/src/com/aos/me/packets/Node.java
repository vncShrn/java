package com.aos.me.packets;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.aos.me.other.Definitions;

/**
 * Neighbour Class used for priorityQ to store and update info about neighbor
 * current processing time
 * 
 * @author Vincy Shrine
 *
 */
public class Node implements Serializable {

	private static final long serialVersionUID = 1L;
	private String hostName;
	private int tcpPortNo;
	private int processId;
	
	// Used only for passing neighbor info
	private Map<Integer, Node> neighborMap;

	public Node(int id, String hostName, int tcpPortNo) {
		this.hostName = hostName;
		this.tcpPortNo = tcpPortNo;
		this.processId = id;
		this.neighborMap = new HashMap<>(Definitions.MAX_PROCESS);
	}

	public Node(int id) {
		this.processId = id;
	}

	public int getPortNo() {
		return tcpPortNo;
	}

	public String getHostName() {
		return hostName;
	}

	public Integer getProcessId() {
		return processId;
	}

	public void setProcessId(int id) {
		this.processId = id;
	}

	public Map<Integer, Node> getNeighbors() {
		return neighborMap;
	}

	public void setNeighbors(Map<Integer, Node> neighborMap) {
		this.neighborMap = neighborMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + processId;
		result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
		result = prime * result + tcpPortNo;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
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
		Node other = (Node) obj;
		if (processId != other.processId)
			return false;
		if (hostName == null) {
			if (other.hostName != null)
				return false;
		} else if (!hostName.equals(other.hostName))
			return false;
		if (tcpPortNo != other.tcpPortNo)
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return processId + "";
	}

}
