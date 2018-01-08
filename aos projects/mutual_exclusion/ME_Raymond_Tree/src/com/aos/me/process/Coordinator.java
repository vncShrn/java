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
import com.aos.me.packets.Node;
import com.aos.me.packets.PacketType;
import com.aos.me.packets.ParentPacket;
import com.aos.me.packets.RegisterPacket;
import com.aos.me.packets.SdPacket;

/**
 * 
 * @author Vincy Shrine
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
	private Map<Integer, Integer> configMap = null;
	private Set<RegisterPacket> registeredSet = new HashSet<>(Definitions.MAX_PROCESS);
	private int processIdCounter = 1;
	private Set<Integer> readySet = new HashSet<>(Definitions.MAX_PROCESS);
	private Set<Integer> completeSet = new HashSet<>(Definitions.MAX_PROCESS);
	private Helper helper = new Helper();
	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	private long allProcessAvgWaitTime = 0;
	private double allProcessAvgMsgCnt = 0;
	private List<SdPacket> sdPacketList = new ArrayList<>();

	public Map<Integer, Integer> getConfigMap() {
		return configMap;
	}

	public void setConfigMap(Map<Integer, Integer> networkMap) {
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
		return readySet.size() == Definitions.MAX_PROCESS - 1;
	}

	public void addToCompleteNodes(Integer senderId) {
		completeSet.add(senderId);
	}

	public boolean isAllProcessComplete() {
		return completeSet.size() == Definitions.MAX_PROCESS;
	}

	public void sendParentInfoToAllProcess(String logPrefix) {
		System.out.println(df.format(new Date()) + logPrefix + "Sending parent details to all process");

		Map<Integer, Node> nodesAndIdMap = new HashMap<>(Definitions.MAX_PROCESS);

		nodesAndIdMap.put(1, new Node(1, hostName, Definitions.COORD_PORT));

		for (RegisterPacket packet : registeredSet)
			nodesAndIdMap.put(packet.getProcessId(),
					new Node(packet.getProcessId(), packet.getHostName(), packet.getPortNo()));

		MeNode.getInstance().setProcessInformation(nodesAndIdMap);
		Map<Integer, Node> parentNodeMap = new HashMap<>(Definitions.MAX_PROCESS);

		for (Entry<Integer, Integer> entry : configMap.entrySet()) {
			Integer nodeId = entry.getKey();
			Integer parentId = entry.getValue();
			Node parent = nodesAndIdMap.get(parentId);
			parentNodeMap.put(nodeId, parent);
		}

		MeNode.getInstance().setParent(parentNodeMap.get(1));

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
					System.out.println(df.format(new Date()) + logPrefix + "Error: Socket Create: " + name + " "
							+ node.getPortNo() + " " + e.getMessage());
					continue;
				}
				Node parent = parentNodeMap.get(node.getProcessId());
				helper.sendTcpMessage(new ParentPacket(parent, nodesAndIdMap), socket, logPrefix + "Send "
						+ PacketType.PARENT + " to => " + entry.getKey() + " parent: " + parent.getProcessId());
			}
		}
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

	public List<SdPacket> getSdPacketList() {
		return sdPacketList;
	}

	public void addToSdPacketList(SdPacket readObject) {
		sdPacketList.add(readObject);
	}
}
