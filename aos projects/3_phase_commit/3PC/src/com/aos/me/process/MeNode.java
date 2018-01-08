package com.aos.me.process;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.aos.me.other.Definitions;
import com.aos.me.other.Node;

/**
 * This is a singleton class to store information about process
 * 
 * @author Vincy Shrine
 *
 */
public class MeNode implements Serializable {

	private Integer processId;
	private String hostName;
	private Integer tcpPortNo;
	private Map<Integer, Node> neighborMap = java.util.Collections
			.synchronizedMap(new HashMap<>(Definitions.MAX_PROCESS));
	private Set<Integer> helloSet = new HashSet<>(Definitions.MAX_PROCESS);
	private Map<Integer, Streams> socketStreamMap = java.util.Collections
			.synchronizedMap(new HashMap<>(Definitions.MAX_PROCESS));
	private ExecutorService consumerQExecutor = Executors.newSingleThreadExecutor();

	private ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(2);

	// private State state = null;

	private static MeNode node;
	private static final long serialVersionUID = 1L;

	public static synchronized MeNode getInstance() {
		if (node == null) {
			node = new MeNode();
		}
		return node;
	}

	private MeNode() {
	}

	public Integer getId() {
		return processId;
	}

	public void setProcessId(int id) {
		this.processId = id;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String ipAddress) {
		this.hostName = ipAddress;
	}

	public Integer getPortNo() {
		return tcpPortNo;
	}

	public void setPortNo(int tcpPortNo) {
		this.tcpPortNo = tcpPortNo;
	}

	public Map<Integer, Node> getNeighbors() {
		return neighborMap;
	}

	public void setNeighbors(Map<Integer, Node> neighbors) {
		this.neighborMap = neighbors;
	}

	public void addToHelloNodes(Integer neighborId) {
		helloSet.add(neighborId);
	}

	public boolean isReceivedAllHello() {
		if (helloSet.isEmpty() || neighborMap.isEmpty())
			return false;
		return helloSet.size() == neighborMap.size();
	}

	public Map<Integer, Streams> getSocketStreamMap() {
		return socketStreamMap;
	}

	public void addToSocketStreamMap(Integer id, Streams streams) {
		this.socketStreamMap.put(id, streams);
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
		MeNode other = (MeNode) obj;
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
		return "Node [id=" + processId + "neighbors=" + neighborMap + "]";
	}

	public ExecutorService getConsumerQThread() {
		return consumerQExecutor;
	}

	public ArrayBlockingQueue<Object> getTokenQ() {
		return queue;
	}

}