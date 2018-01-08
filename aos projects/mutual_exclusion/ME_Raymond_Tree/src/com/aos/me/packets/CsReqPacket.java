package com.aos.me.packets;

import com.aos.me.other.Definitions;

/**
 * 
 * @author Vincy Shrine
 * @author Dhwani Raval
 */
public class CsReqPacket implements Packet {

	public CsReqPacket() {
		counter = counter + 1;
		csReqId = counter;
	}

	private static final long serialVersionUID = 1L;

	private Integer senderId = Definitions.PID;
	private int csReqId = 0;

	private static int counter = 0;

	public Integer getSenderId() {
		return senderId;
	}

	@Override
	public String toString() {
		return "CS_REQ [sender=" + senderId + "req#=" + csReqId + "]";
	}

	public int getCsReqId() {
		return csReqId;
	}

	/** Dhwani - Begin */
	
	private String csRequestString;
	private int messageCount = 0;

	public void setSenderId(Integer senderId) {
		this.senderId = senderId;
	}

	public int getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
	}

	public String getCsRequestString() {
		return csRequestString;
	}

	public void setCsRequestString(String csRequestString) {
		this.csRequestString = csRequestString;
	}

	/** Dhwani -End */

}
