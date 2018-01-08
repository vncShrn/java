package com.acn.fog.listener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.acn.fog.other.ConfigReader;

public class Packet implements Serializable {

	public Packet(long seqNo, int requestType, int forwardLimit, String iotIpAddress, int iotPortNo) {
		super();
		this.seqNo = seqNo;
		this.requestType = requestType;
		this.processingTime = (ConfigReader.getReqMap()).get(requestType);
		this.forwardLimit = forwardLimit;
		this.iotIpAddress = iotIpAddress;
		this.iotPortNo = iotPortNo;
		this.id = iotIpAddress + ":" + iotPortNo;
	}

	private static final long serialVersionUID = 1L;

	private long seqNo;
	// there are 10 message types
	private int requestType;
	private int forwardLimit;
	private String iotIpAddress;
	private int iotPortNo;
	/** for logging purposes */
	private String id;

	// Additional fields
	private Long processingTime;
	private List<Integer> pathTraversed = new ArrayList<>();

	private String logs = "";

	private int currentForwardLimit = 0;

	public long getSeqNo() {
		return seqNo;
	}

	public int getRequestType() {
		return requestType;
	}

	public int getForwardLimit() {
		return forwardLimit;
	}

	public String getIotIpAddress() {
		return iotIpAddress;
	}

	public int getIotPortNo() {
		return iotPortNo;
	}

	public List<Integer> getPathTraversed() {
		return pathTraversed;
	}

	public void addToPathTraversed(int id) {
		pathTraversed.add(id);
	}

	public void addLog(String log) {
		logs += log;
	}

	public Long getProcessingTime() {
		return processingTime;
	}

	@Override
	public String toString() {
		return "Packet [id=" + id + ", T=" + requestType + ", PT=" + processingTime + ", FL=" + forwardLimit
				+ ", \nseq#: " + seqNo + ", path=" + pathTraversed + ", details=" + logs + "]";
	}

	public String getId() {
		return id;
	}

	public void setCurrentForwardLimit(int limit) {
		currentForwardLimit = limit;
	}

	public int getCurrentForwardLimit() {
		return currentForwardLimit;
	}

}
