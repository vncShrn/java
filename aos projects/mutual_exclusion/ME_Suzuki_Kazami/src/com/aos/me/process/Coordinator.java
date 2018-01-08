package com.aos.me.process;

import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.packets.HelloPacket;
import com.aos.me.packets.NeighborsPacket;
import com.aos.me.packets.Node;
import com.aos.me.packets.PacketType;
import com.aos.me.packets.RegisterPacket;
import com.aos.me.packets.SdPacket;

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
	private String hostName;
	private Map<Integer, List<Integer>> configMap = null;
	private Set<RegisterPacket> registeredSet = new HashSet<>(Definitions.MAX_PROCESS);
	private int processIdCounter = 1;
	private Set<Integer> readySet = new HashSet<>(Definitions.MAX_PROCESS);
	private Set<Integer> completeSet = new HashSet<>(Definitions.MAX_PROCESS);
	private Helper helper = new Helper();
	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	private long allProcessAvgWaitTime = 0;
	private double allProcessAvgMsgCnt = 0;
	private List<SdPacket> sdPacketList = new ArrayList<>();

	public Map<Integer, List<Integer>> getConfigMap() {
		return configMap;
	}

	public void setConfigMap(Map<Integer, List<Integer>> networkMap) {
		this.configMap = networkMap;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public RegisterPacket addToRegisteredNodes(String hostName, int port) {
		RegisterPacket packet = new RegisterPacket(hostName, port);
		packet.setProcessId(++processIdCounter);
		registeredSet.add(packet);
		return packet;
	}

	public boolean isRegistrationComplete() {
		return registeredSet.size() == Definitions.MAX_PROCESS - 1;
	}

	public void addToReadyNodes(Integer senderId) {
		readySet.add(senderId);
	}

	public boolean isAllProcessReady() {
		return readySet.size() == Definitions.MAX_PROCESS;
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

	public void addToAvgWaitTime(long avgWaitTime) {
		this.allProcessAvgWaitTime += avgWaitTime;
	}

	public void addToAvgMsgCount(double avgNumberOfMsgs) {
		this.allProcessAvgMsgCnt += avgNumberOfMsgs;
	}

	public long getAllProcessAvgWaitTime() {
		return allProcessAvgWaitTime;
	}

	public double getAllProcessAvgMsgCnt() {
		return allProcessAvgMsgCnt;
	}

	public void sendNeighborInfoToAllProcess(String logPrefix) {

		System.out.println(df.format(new Date()) + logPrefix + "Sending neighbor details to all process");
		Map<Integer, Node> nodesAndIdMap = new HashMap<>(Definitions.MAX_PROCESS);
		nodesAndIdMap.put(1, new Node(1, hostName, Definitions.COORD_PORT));

		for (RegisterPacket packet : registeredSet)
			nodesAndIdMap.put(packet.getProcessId(),
					new Node(packet.getProcessId(), packet.getHostName(), packet.getPortNo()));

		for (Entry<Integer, List<Integer>> entry : configMap.entrySet()) {
			Integer nodeId = entry.getKey();
			List<Integer> neighborList = entry.getValue();
			// build neighbors for node
			Node node = nodesAndIdMap.get(nodeId);
			Map<Integer, Node> neighborNodes = new HashMap<>(Definitions.MAX_PROCESS);
			for (Integer neighId : neighborList) {
				Node neighbor = nodesAndIdMap.get(neighId);
				neighborNodes.put(neighId, neighbor);
			}
			node.setNeighbors(neighborNodes);
		}
		MeNode.getInstance().setNeighbors(nodesAndIdMap.get(1).getNeighbors());
		helper.sendPacketToAllNeighbors(logPrefix, PacketType.HELLO, new HelloPacket());
		for (Entry<Integer, Node> entry : nodesAndIdMap.entrySet()) {
			Node node = entry.getValue();
			String name = node.getHostName();
			if (name.equals(hostName) && node.getPortNo() == Definitions.COORD_PORT && Definitions.AM_I_COORDINATOR) {
				continue;
			} else {
				Socket socket = null;
				try {
					socket = new Socket(name, node.getPortNo());
				} catch (IOException e) {
					System.out.println(df.format(new Date()) + logPrefix + "Error: Socket Create: " + e.getMessage());
					continue;
				}
				helper.sendTcpMessage(new NeighborsPacket(node), socket,
						logPrefix + "Send " + PacketType.NEIGHBOR + " to => " + entry.getKey());

			}
		}
	}

	public List<SdPacket> getSdPacketList() {
		return sdPacketList;
	}

	public void addToSdPacketList(SdPacket readObject) {
		sdPacketList.add(readObject);
	}

}
