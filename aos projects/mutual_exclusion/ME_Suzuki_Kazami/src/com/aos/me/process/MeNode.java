package com.aos.me.process;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.aos.me.other.Definitions;
import com.aos.me.packets.Node;
import com.aos.me.packets.TokenPacket;

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
	private Map<Integer, ObjectOutputStream> socketStreamMap = java.util.Collections
			.synchronizedMap(new HashMap<>(Definitions.MAX_PROCESS));
	private ExecutorService consumerQExecutor = Executors.newSingleThreadExecutor();

	private volatile boolean isTokenWithMe = false;
	private volatile boolean isCsExecutionOngoing = false;

	private BlockingQueue<TokenPacket> tokenQ = new LinkedBlockingQueue<>();

	private List<Integer> RN = new ArrayList<>(Collections.nCopies(Definitions.MAX_PROCESS, 0));
	private TokenPacket token = null;
	private int totalCsReqMsgsSent = 0;

	private static MeNode node;
	private static final long serialVersionUID = 1L;

	public static synchronized MeNode getInstance() {
		if (node == null) {
			node = new MeNode();
		}
		return node;
	}

	private MeNode() {
		// Initially the token is with the Coodinator
		if (Definitions.AM_I_COORDINATOR)
			isTokenWithMe = true;
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

	public Map<Integer, ObjectOutputStream> getSocketStreamMap() {
		return socketStreamMap;
	}

	public void addToSocketStreamMap(Integer id, ObjectOutputStream stream) {
		this.socketStreamMap.put(id, stream);
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

	public boolean isTokenWithMe() {
		return isTokenWithMe;
	}

	public void setTokenWithMe(boolean isTokenWithMe) {
		this.isTokenWithMe = isTokenWithMe;
	}

	public boolean isCsExecutionOngoing() {
		return isCsExecutionOngoing;
	}

	public void setCsExecutionOngoing(boolean isCsExecutionOngoing) {
		this.isCsExecutionOngoing = isCsExecutionOngoing;
	}

	public Integer getRN(int index) {
		return RN.get(index - 1);
	}

	public List<Integer> getRNList() {
		return RN;
	}

	public void setRN(int index, int val) {
		RN.set(index - 1, val);
		System.out.println("** RN(" + index + ")=" + RN.get(index - 1));
	}

	public int incrementRN(int index) {
		RN.set(index - 1, RN.get(index - 1) + 1);
		System.out.println("** RN(" + index + ")=" + RN.get(index - 1));
		return RN.get(index - 1);
	}

	public BlockingQueue<TokenPacket> getTokenQ() {
		return tokenQ;
	}

	public TokenPacket getToken() {
		return token;
	}

	public void setToken(TokenPacket token) {
		this.token = token;
	}

	public void incrementTotalCsReqMsgsSent() {
		totalCsReqMsgsSent++;
	}

	public int getTotalCsReqMsgsSent() {
		return totalCsReqMsgsSent;
	}

}