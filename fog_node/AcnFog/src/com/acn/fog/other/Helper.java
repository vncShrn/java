package com.acn.fog.other;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.acn.fog.listener.Packet;
import com.acn.fog.resptime.FogNode;
import com.acn.fog.resptime.NeighborNode;

public class Helper {

	public static boolean isProcessable(long packetProcessingTime, long fogCurrentProcessingTime) {

		long maxResponseTimeFog = (FogNode.getInstance()).getMaxResponseTime();
		if (fogCurrentProcessingTime + packetProcessingTime <= maxResponseTimeFog)
			return true;
		return false;
	}

	public static synchronized boolean offload(Packet packet, String logPrefix) {
		try {
			FogNode fogNode = FogNode.getInstance();
			if (packet.getCurrentForwardLimit() >= packet.getForwardLimit()) {
				System.out.println(
						logPrefix + "This packet has crossed the maximum Fog forward limit. Hence offload to cloud");
				packet.addLog("FL reached; ");
				return false;
			}
			packet.addLog("FL: " + packet.getCurrentForwardLimit()+"; ");

			if (fogNode.getNeighborQ() == null) {
				System.err.println(logPrefix + "Neighbor fogs unavailable");
				packet.addLog("neighbors unavail; ");
				return false;
			}

			PriorityQueue<NeighborNode> tempQ = new PriorityQueue<>(fogNode.getNeighborQ());
			// Pick the best neighbor
			NeighborNode bestNeighbor = null;
			System.out.println("PQ: " + tempQ.toString());
			packet.addLog("neigh PQ: " + tempQ.toString() + "; ");
			String visited = "";
			while (tempQ != null && tempQ.peek() != null) {
				NeighborNode neighbor = tempQ.poll();
				if (neighbor != null)
					if (packet.getPathTraversed().contains(neighbor.getId())) {
						visited += neighbor.getId() + " ";
						System.out.println(logPrefix + "This packet was already processed by neighbor fog Node: "
								+ neighbor.getId());
						// packet.addToPathTraversed("Already served by:
						// "+neighbor.getId()+"; ");
					} else {
						bestNeighbor = neighbor;
						break;
					}
			}
			if (!"".equals(visited))
				packet.addLog(visited + "visited; ");

			if (bestNeighbor == null) {
				// no best neighbor found, so offload to cloud
				System.out.println(logPrefix + "No best neighbor found, so offload to cloud");
				packet.addLog("all neighbors visited; ");
				return false;
			} else {
				System.out.println(logPrefix + "Picked the best neighbor as: " + bestNeighbor.toString());
				packet.addLog("offload to: " + bestNeighbor.getId() + "; ");
				// offload to the best neighbor
				try {
					int fl = packet.getCurrentForwardLimit();
					packet.setCurrentForwardLimit(++fl);
					sendTcpMessage(packet, bestNeighbor.getIpAddress(), bestNeighbor.getTcpPortNo(), logPrefix);
				} catch (IOException e) {
					System.out.println(logPrefix + "Error encountered when offloading to fog: " + bestNeighbor.getId());
					packet.addLog("offload failed; ");
					return false;
				}
				return true;
			}
		} catch (Exception e) {
			System.err.println("Error encountered when offloading to neighbor fog: " + e.getMessage());
			packet.addLog("offload failed; ");
			return false;
		}
	}

	public static synchronized void sendTcpMessage(Object msg, String ipAddress, int portNo, String logPrefix)
			throws IOException {
		Socket clientSocket = null;
		try {
			// Create a client socket and connect to server
			clientSocket = new Socket(InetAddress.getByName(ipAddress), portNo);
			OutputStream os = clientSocket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(msg);
			clientSocket.shutdownOutput();
			System.out.println(logPrefix + "Sent msg to fog: " + ipAddress + ":" + portNo);
			oos.flush();
			oos.close();
		} finally {
			if (clientSocket != null)
				try {
					clientSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public static synchronized void sendUdpResponse(byte[] data, String ipAddress, int port, String logPrefix) {

		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
			DatagramPacket sendPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ipAddress), port);
			clientSocket.send(sendPacket);
			System.out.println(logPrefix + "UDP response sent to IOT: " + ipAddress + ":" + port);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (clientSocket != null)
				clientSocket.close();
		}
	}

	public static class FogCurrentProcessingTimeComparator implements Comparator<NeighborNode> {

		@Override
		public synchronized int compare(NeighborNode a, NeighborNode b) {
			int compTimeResult = (a.getCurrentProcessingTime()).compareTo(b.getCurrentProcessingTime());
			if (compTimeResult != 0) {
				return compTimeResult;
			}
			return (a.getId()).compareTo(b.getId());
		}
	}
}
