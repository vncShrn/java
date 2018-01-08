package com.aos.me.packets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.aos.me.other.Definitions;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class TokenPacket implements Packet {


	public TokenPacket() {
		tokenQ = new ConcurrentLinkedQueue<>();
		LN = new ArrayList<>(Collections.nCopies(Definitions.MAX_PROCESS, 0));
	}

	private static final long serialVersionUID = 1L;

	private Integer senderId = Definitions.PID;
	private ConcurrentLinkedQueue<Integer> tokenQ;
	private ArrayList<Integer> LN;


	public Integer getSenderId() {
		return senderId;
	}
	
	public void setSenderId(int id) {
		senderId = id;
	}
	
	public ConcurrentLinkedQueue<Integer> getTokenQ() {
		return tokenQ;
	}

	public void addToTokenQ(Integer processId) {
		this.tokenQ.add(processId);
	}

	public Integer getLN(int index) {
		return LN.get(index-1);
	}

	public void setLN(int index, int val) {
		LN.set(index-1, val);
		System.out.println("** LN("+index+")="+LN.get(index-1));
	}

	@Override
	public String toString() {
		return "TOKEN [sender=" + senderId + ", tokenQ=" + tokenQ + ", LN=" + LN + "]";
	}

	public List<Integer> getLNList() {
		return LN;
	}


}
