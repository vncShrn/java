package com.aos.me.packets;

import com.aos.me.other.Definitions;

/**
 * 
 * @author Vincy Shrine
 */
public class MeCompletePacket implements Packet {

	public MeCompletePacket() {
		this.senderId = Definitions.PID;
	}

	private static final long serialVersionUID = 1L;

	private Integer senderId;

	private long avgWaitTime;

	private double avgMsgCount;

	public Integer getSenderId() {
		return senderId;
	}

	public long getAvgWaitTime() {
		return avgWaitTime;
	}

	public void setAvgWaitTime(long avgWaitTime) {
		this.avgWaitTime = avgWaitTime;
	}

	public double getAvgMsgCnt() {
		return avgMsgCount;
	}

	public void setAvgMsgCnt(double cnt) {
		this.avgMsgCount = cnt;
	}

}
