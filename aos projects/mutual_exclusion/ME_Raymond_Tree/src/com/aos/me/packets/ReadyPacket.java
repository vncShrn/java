package com.aos.me.packets;

import com.aos.me.other.Definitions;

/**
 * 
 * @author Vincy Shrine
 */
public class ReadyPacket implements Packet {

	public ReadyPacket() {
		this.senderId = Definitions.PID;
	}

	private static final long serialVersionUID = 1L;

	private Integer senderId;

	public Integer getSenderId() {
		return senderId;
	}

}
