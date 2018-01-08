package com.aos.me.packets;

import com.aos.me.other.Definitions;
import com.aos.me.types.Packet;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class CommitPacket implements Packet {

	public CommitPacket() {
		this.senderId = Definitions.PID;
	}

	private static final long serialVersionUID = 1L;

	private Integer senderId;

	public Integer getSenderId() {
		return senderId;
	}

}
