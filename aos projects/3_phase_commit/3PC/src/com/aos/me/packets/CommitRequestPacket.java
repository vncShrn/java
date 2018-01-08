package com.aos.me.packets;

import com.aos.me.other.Definitions;
import com.aos.me.types.Packet;
import com.aos.me.types.StateT;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class CommitRequestPacket implements Packet {

	/**
	 * No failures
	 * 
	 * @param transactionId
	 * @param commitValue
	 */
	public CommitRequestPacket(int transactionId, int commitValue) {
		this.senderId = Definitions.PID;
		this.transactionId = transactionId;
		this.commitValue = commitValue;
	}

	/**
	 * Cohort replies abort
	 * 
	 * @param transactionId
	 * @param commitValue
	 * @param cohortId
	 * @param replyAbort
	 * @param failAtState
	 */
	public CommitRequestPacket(int transactionId, int cohortId, int commitValue) {
		this.senderId = Definitions.PID;
		this.transactionId = transactionId;
		this.commitValue = commitValue;
		this.replyAbort = true;
		this.cohortId = cohortId;
	}
	
	/**
	 * Coord fails at state
	 * 
	 * @param transactionId
	 * @param commitValue
	 * @param isCoord
	 * @param failAtState
	 */
	public CommitRequestPacket(int transactionId, StateT failAtState, int commitValue) {
		this.senderId = Definitions.PID;
		this.transactionId = transactionId;
		this.commitValue = commitValue;
		this.failAtState = failAtState;
		this.isCoord = true;
	}

	/**
	 * Cohort fails at state
	 * 
	 * @param transactionId
	 * @param commitValue
	 * @param cohortId
	 * @param failAtState
	 */
	public CommitRequestPacket(int transactionId, int cohortId, StateT failAtState, int commitValue) {
		this.senderId = Definitions.PID;
		this.transactionId = transactionId;
		this.commitValue = commitValue;
		this.failAtState = failAtState;
		this.cohortId = cohortId;
	}

	private static final long serialVersionUID = 1L;

	private Integer senderId;

	private int transactionId;

	private int commitValue;

	private boolean isCoord = false;

	private int cohortId;

	private boolean replyAbort;
	private StateT failAtState;

	public Integer getSenderId() {
		return senderId;
	}

	public int getTransactionId() {
		return transactionId;
	}

	public int getCommitValue() {
		return commitValue;
	}

	public boolean isCoord() {
		return isCoord;
	}

	@Override
	public String toString() {
		return "[id=" + transactionId + ", val=" + commitValue + ", isCoord=" + isCoord + ", replyAbort="
				+ replyAbort + ", failAtState=" + failAtState + "]";
	}

	public boolean isReplyAbort() {
		return replyAbort;
	}

	public StateT getFailAtState() {
		return failAtState;
	}

	public int getCohortId() {
		return cohortId;
	}

}
