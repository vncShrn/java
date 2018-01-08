package com.aos.me.process;

import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.aos.me.other.Definitions;
import com.aos.me.packets.CsReqPacket;
import com.aos.me.packets.Node;
import com.aos.me.packets.TokenPacket;

/**
 * This is a singleton class to store information about process
 * 
 * @author Vincy Shrine
 * @author Dhwani Raval
 * 
 */
public class MeNode implements Serializable {

	private Integer processId;
	private String hostName;
	private Integer tcpPortNo;
	private Map<Integer, ObjectOutputStream> socketStreamMap = java.util.Collections
			.synchronizedMap(new HashMap<>(Definitions.MAX_PROCESS));
	private ExecutorService consumerQExecutor = Executors.newSingleThreadExecutor();

	private Map<Integer, Node> processInformation = new HashMap<>(Definitions.MAX_PROCESS);

	private volatile boolean isTokenWithMe = false;
	private volatile boolean isCsExecutionOngoing = false;

	private TokenPacket token = null;
	private Node parent = null;

	// process requesting CS
	private ConcurrentLinkedQueue<Integer> requestQ = new ConcurrentLinkedQueue<>();
	// to enter CS
	private ArrayBlockingQueue<TokenPacket> tokenQ = new ArrayBlockingQueue<>(1);

	private static MeNode node;
	private static final long serialVersionUID = 1L;

	/** Dhwani */
	private Map<String, Integer> successfulCsRequestMap = java.util.Collections.synchronizedMap(new HashMap<>());
	private Map<Integer, CsReqPacket> csRequestMap = java.util.Collections.synchronizedMap(new HashMap<>());

	/** Dhwani */

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

	public Node getParent() {
		return parent;
	}

	public void setParent(Node node) {
		this.parent = node;
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
		return "Node [id=" + processId + "parent=" + node.getId() + "]";
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

	public ArrayBlockingQueue<TokenPacket> getTokenQ() {
		return tokenQ;
	}

	public TokenPacket getToken() {
		return token;
	}

	public void setToken(TokenPacket token) {
		this.token = token;
	}

	public Map<Integer, Node> getProcessInformation() {
		return processInformation;
	}

	public void setProcessInformation(Map<Integer, Node> processInformation) {
		this.processInformation = processInformation;
	}

	public ConcurrentLinkedQueue<Integer> getRequestQ() {
		return requestQ;
	}

	/** Dhwani - Begin */
	public Map<String, Integer> getSuccessfulCsRequestMap() {
		return successfulCsRequestMap;
	}

	public Map<Integer, CsReqPacket> getCsRequestMap(){
		return csRequestMap;
	}
	
	public void addToCsRequestMap(Integer pId, CsReqPacket csRequestPacket) {
		csRequestMap.put(pId, csRequestPacket);
	}

	public void removeFromCsRequestMap(Integer pId) {
		csRequestMap.remove(pId);
	}

	public synchronized void incrementMessageCount(Integer pId) {
		CsReqPacket csRequestPacket = csRequestMap.get(pId);
		csRequestPacket.setMessageCount(csRequestPacket.getMessageCount() + 1);
		/** Not necessary, objects are passed by reference - Vincy */
		// csRequestMap.replace(pId, csRequestPacket);
	}

	public void addToSuccessfulCsRequestMap(String csRequest, Integer messageCount) {
		successfulCsRequestMap.put(csRequest, messageCount);
	}

	public int getSuccessfulCsRequestFromMap(String csRequestString) {
		return successfulCsRequestMap.get(csRequestString);
	}
	/** Dhwani -End */
}