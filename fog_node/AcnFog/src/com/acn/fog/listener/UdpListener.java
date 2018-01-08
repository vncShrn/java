package com.acn.fog.listener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;

import com.acn.fog.other.ConfigReader;
import com.acn.fog.other.FogDefinitions;
import com.acn.fog.other.Helper;
import com.acn.fog.resptime.FogNode;

public class UdpListener implements Runnable {

	private FogNode fogNode = null;
	private ArrayBlockingQueue<Packet> fogQueue = null;
	private ArrayBlockingQueue<Packet> cloudQueue;

	public UdpListener(ArrayBlockingQueue<Packet> fogQueue, ArrayBlockingQueue<Packet> cloudQueue) {
		this.fogNode = FogNode.getInstance();
		this.fogQueue = fogQueue;
		this.cloudQueue = cloudQueue;
	}

	@Override
	public void run() {

		String logPrefix = "Fog " + fogNode.getId() + ": FOG-UDP LISTENER- ";
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(fogNode.getUdpPortNo());
			byte[] receiveData = new byte[FogDefinitions.UDP_BUFFER_SIZE];
			while (true) {

				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); // receive
																									// the
																									// response
				serverSocket.receive(receivePacket);

				// 1. Tokenize received IOT packet into object
				String iotPacketString = new String(receivePacket.getData(), receivePacket.getOffset(),
						receivePacket.getLength());
				Packet packet = tokenizeIotIntoPacket(iotPacketString);
				System.out.println(logPrefix + "Incoming IOT req packet tokenized: " + packet.toString());

				// 2. add fogid to visited list of packet
				packet.addToPathTraversed(fogNode.getId());
				packet.addLog("\nVisited " + fogNode.getId() + "; ");

				// 3. Check if the packet can be processed by this Fog Node
				long packetProcessingTime = (ConfigReader.getReqMap()).get(packet.getRequestType());
				packet.addLog(
						"available PT: " + (fogNode.getMaxResponseTime() - fogNode.getCurrentProcessingTime()) + "; ");
				boolean isProcessable = Helper.isProcessable(packetProcessingTime, fogNode.getCurrentProcessingTime());

				logPrefix = logPrefix + " packet: " + packet.getId() + " #"+ packet.getSeqNo() + " -";
				if (isProcessable) {
					fogNode.addToCurrentProcessingTime(packetProcessingTime);
					try {
						fogQueue.put(packet);
						packet.addLog("can process; "); 
						System.out.println(logPrefix + "IOT request is processable. Hence added to fog Queue");
					} catch (InterruptedException e) {
						System.err.println(logPrefix + "Fog Producer Queue error: " + e.getMessage());
					}
				} else {
					// if current fog node cant process, offload to best
					// neighbor
					System.out.println(logPrefix
							+ "IOT request is not processable by this fog. Hence try to offload to best neighbor fog");
					packet.addLog("cannot process; "); 
					boolean status = Helper.offload(packet, logPrefix);
					// else offload to cloud
					if (!status) {
						try {
							cloudQueue.put(packet);
							System.out.println(logPrefix
									+ "IOT request offload to best neighbor fog failed. Hence added to cloud Queue");
						} catch (InterruptedException e) {
							System.err.println(logPrefix + "Adding packet to cloud Queue failed: " + e.getMessage());
						}
					}
				}
				logPrefix = "Fog " + fogNode.getId() + ": FOG-UDP LISTENER- ";
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (serverSocket != null)
				serverSocket.close();
		}
	}

	private Packet tokenizeIotIntoPacket(String iotPacketString) {

		long seqNo = 0;
		int requestType = 0;
		int forwardLimit = 0;
		String iotIpAddress = null;
		int iotPortNo = 0;
		String[] iotArr = iotPacketString.split("\\s+");
		for (String element : iotArr) {
			String[] value = element.split(":");
			switch (value[0]) {

			case "#":
				seqNo = Integer.parseInt(value[1]);
				break;
			case "T":
				requestType = Integer.parseInt(value[1]);
				break;
			case "FL":
				forwardLimit = Integer.parseInt(value[1]);
				break;
			case "IP":
				iotIpAddress = value[1];
				break;
			case "P":
				iotPortNo = Integer.parseInt(value[1]);
				break;
			}
		}

		Packet packet = new Packet(seqNo, requestType, forwardLimit, iotIpAddress, iotPortNo);
		return packet;
	}
}
