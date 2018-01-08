package com.aos.me.packets;

import com.aos.me.other.Definitions;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class CsReqPacket implements Packet {

	public CsReqPacket(int RNi) {
		this.RNi = RNi;
		counter = counter + 1;
		csReqId = counter;
	}

	private static final long serialVersionUID = 1L;

	private Integer senderId = Definitions.PID;
	private int RNi;
	private int csReqId = 0;

	private static int counter = 0;

	public Integer getSenderId() {
		return senderId;
	}

	@Override
	public String toString() {
		return "CS_REQ [sender=" + senderId + " req#="+csReqId+"]";
	}

	public int getRNi() {
		return RNi;
	}

	public int getCsReqId() {
		return csReqId;
	}

}
