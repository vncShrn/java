package com.aos.me.packets;

import com.aos.me.other.Definitions;

/**
 * 
 * @author Vincy Shrine
 * @author Dhwani Raval
 */
public class TokenPacket implements Packet {

	private static final long serialVersionUID = 1L;

	private Integer senderId = Definitions.PID;
	private boolean isSenderToBeAddedToQ = false;

	public Integer getSenderId() {
		return senderId;
	}

	public void setSenderId(Integer id) {
		senderId = id;
	}

	@Override
	public String toString() {
		return "TOKEN [sender=" + senderId + " isSenderToBeAddedToQ= " + isSenderToBeAddedToQ + "]";
	}

	public boolean isSenderToBeAddedToQ() {
		return isSenderToBeAddedToQ;
	}

	public void setSenderToBeAddedToQ(boolean isSenderToBeAddedToQ) {
		this.isSenderToBeAddedToQ = isSenderToBeAddedToQ;
	}

	/** Dhwani - Begin */
	
	private CsReqPacket csRequest, nextCsRequest;

	public CsReqPacket getCsRequest() {
		return csRequest;
	}

	public void setCsRequest(CsReqPacket csRequest) {
		this.csRequest = csRequest;
	}

	public CsReqPacket getNextCsRequest() {
		return nextCsRequest;
	}

	public void setNextCsRequest(CsReqPacket nextCsRequest) {
		this.nextCsRequest = nextCsRequest;
	}
	/** Dhwani - End */

}
