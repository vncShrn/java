package com.aos.me.types;

public enum StateT {

	/**
	 * q state <br>
	 * coordinator has received transaction requesting COMMIT <br>
	 * cohort has received COMMIT_REQ from cooridnator
	 */
	REQ_RECEIVED("q"),

	/**
	 * w state<br>
	 * coordinator has sent COMMIT_REQ to all cohorts, and is waiting for reply
	 * AGREED/ABORT <br cohort has sent AGREED, and is waiting for PREPARE/ABORT
	 * 
	 */
	WAIT("w"),

	/**
	 * p state <br>
	 * coordinator has sent PREPARE to all cohorts, and is waiting for ACK <br>
	 * cohort has received PREPARE, ACK sent, waiting for COMMIT
	 * 
	 */
	PREPARE("p"),

	/**
	 * a state <br>
	 * coordinator has received ABORT from some cohort <br>
	 * cohort decided to ABORT on receiving COMMIT_REQ, or later received ABORT
	 * from coord
	 * 
	 */
	ABORT("a"),

	/**
	 * c state <br>
	 * coordinator reeived ACK from all cohort for PREPARE, so sent COMMIT to
	 * all <br>
	 * cohort received COMMIT from coord
	 * 
	 */
	COMMIT("c");

	public static StateT getState(String text) {
		for (StateT s : StateT.values()) {
			if ((s.getVal()).equalsIgnoreCase(text)) {
				return s;
			}
		}
		return null;
	}

	private String val;

	StateT(String text) {
		this.val = text;
	}

	public String getVal() {
		return val;
	}

}
