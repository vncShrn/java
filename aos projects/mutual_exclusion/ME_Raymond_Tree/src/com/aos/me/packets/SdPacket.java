package com.aos.me.packets;

import com.aos.me.other.Definitions;

public class SdPacket implements Packet {

	private String entryTime;

	private String exitTime;

	private int reqId;

	public SdPacket(int reqId, String entryTime, String exitTime) {
		this.entryTime = entryTime;
		this.exitTime = exitTime;
		this.reqId = reqId;
	}

	private static final long serialVersionUID = 1L;

	private Integer senderId = Definitions.PID;

	public Integer getSenderId() {
		return senderId;
	}

	public String getExitTime() {
		return exitTime;
	}

	public String getEntryTime() {
		return entryTime;
	}

	public int getReqId() {
		return reqId;
	}

	@Override
	public String toString() {
		return "SdPacket [pid: " + senderId + " req: " + reqId + " entryTime= " + entryTime + ", exitTime= " + exitTime + "]";
	}

}
