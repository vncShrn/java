package com.aos.me.process;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.aos.me.other.Definitions;
import com.aos.me.other.Node;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class Coordinator {

	public static synchronized Coordinator getInstance() {
		if (coordinator == null) {
			coordinator = new Coordinator();
		}
		return coordinator;
	}

	private Coordinator() {
	}

	private static Coordinator coordinator;
	private Set<Integer> readySet = new HashSet<>(Definitions.MAX_PROCESS);
	private Set<Integer> completeSet = new HashSet<>(Definitions.MAX_PROCESS);
	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	private Set<Integer> agreedSet = new HashSet<>(Definitions.MAX_PROCESS);
	private Set<Integer> ackSet = new HashSet<>(Definitions.MAX_PROCESS);

	public void addToReadyNodes(Integer senderId) {
		readySet.add(senderId);
	}

	public boolean isAllProcessReady() {
		return readySet.size() == Definitions.MAX_PROCESS-1;
	}

	public void addToCompleteNodes(Integer senderId) {
		completeSet.add(senderId);
	}

	public Set<Integer> getCompleteSet() {
		return completeSet;
	}

	public boolean isAllProcessComplete() {
		return completeSet.size() == Definitions.MAX_PROCESS;
	}

	public void addToAgreedNodes(Integer senderId) {
		agreedSet.add(senderId);
	}

	public Set<Integer> getAgreedSet() {
		return agreedSet;
	}
	
	public void resetAgreedSet(){
		agreedSet.clear();
	}

	public boolean isAllProcessAgreed() {
		return agreedSet.size() == Definitions.MAX_PROCESS - 1;
	}

	public void addToAckNodes(Integer senderId) {
		ackSet.add(senderId);
	}

	public Set<Integer> getAckSet() {
		return ackSet;
	}

	public boolean isAllProcessAck() {
		return ackSet.size() == Definitions.MAX_PROCESS - 1;
	}
	
	public void resetAckSet(){
		ackSet.clear();
	}

}
