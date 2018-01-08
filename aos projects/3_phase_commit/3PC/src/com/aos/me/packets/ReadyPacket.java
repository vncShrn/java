package com.aos.me.packets;

public class ReadyPacket implements Packet {

	public ReadyPacket(Integer processId) {
		this.senderId = processId;
	}

	private static final long serialVersionUID = 1L;

	private Integer senderId;

	public Integer getSenderId() {
		return senderId;
	}

}
