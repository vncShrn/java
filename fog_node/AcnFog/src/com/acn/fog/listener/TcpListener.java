package com.acn.fog.listener;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;

import com.acn.fog.other.ConfigReader;
import com.acn.fog.other.FogDefinitions;
import com.acn.fog.other.Helper;
import com.acn.fog.resptime.FogNode;
import com.acn.fog.resptime.NeighborNode;

public class TcpListener implements Runnable {

	private ArrayBlockingQueue<Packet> fogQ;
	private ArrayBlockingQueue<Packet> cloudQ;

	public TcpListener(ArrayBlockingQueue<Packet> fogQueue, ArrayBlockingQueue<Packet> cloudQueue) {
		fogQ = fogQueue;
		cloudQ = cloudQueue;
	}

	@Override
	public void run() {

		FogNode fogNode = FogNode.getInstance();
		String logPrefix = "Fog " + fogNode.getId() + ": FOG-TCP LISTENER- ";
		ServerSocket serverSock = null;
		try {
			serverSock = new ServerSocket(fogNode.getTcpPortNo());
			System.out.println(logPrefix + "Waiting for incoming connections..");
			while (true) {
				// Listens for a connection to be made to this socket and
				// accepts it
				// The method blocks until a connection is made
				Socket sock = serverSock.accept();
				sock.setReuseAddress(true);
				InputStream is = sock.getInputStream();
				ObjectInputStream ois = new ObjectInputStream(is);

				Object readObj = ois.readObject();
				if (readObj instanceof Packet) {
					Packet packet = (Packet) readObj;
					System.out.println(
							logPrefix + "Received OFFLOAD message from neighbor Fog for packet: " + packet.toString());

					logPrefix = logPrefix + " packet: " + packet.getId() + " #"+ packet.getSeqNo() + " -";

					// process the offload req
					// 2. add fogid to visited list of packet
					packet.addToPathTraversed(fogNode.getId());
					packet.addLog("\nVisited " + fogNode.getId() + "; ");

					// 3. Check if the packet can be processed by this Fog Node
					long packetProcessingTime = (ConfigReader.getReqMap()).get(packet.getRequestType());
					packet.addLog("available PT: " + (fogNode.getMaxResponseTime() - fogNode.getCurrentProcessingTime())
							+ "; ");
					boolean isProcessable = Helper.isProcessable(packetProcessingTime,
							fogNode.getCurrentProcessingTime());

					if (isProcessable) {
						fogNode.addToCurrentProcessingTime(packetProcessingTime);
						try {
							fogQ.put(packet);
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
								cloudQ.put(packet);
								System.out.println(logPrefix
										+ "IOT request offload to best neighbor fog failed. Hence added to cloud Queue");
							} catch (InterruptedException e) {
								System.err
										.println(logPrefix + "Adding packet to cloud Queue failed: " + e.getMessage());
							}
						}
					}
				} else if (readObj instanceof FogNode) {
					FogNode fogNodeMsg = (FogNode) readObj;
					System.out.println(
							logPrefix + "Received a RESPONSE TIME update from neighbor fog: " + fogNodeMsg.getId());

					logPrefix = logPrefix + "RT UPDATE- " + fogNodeMsg.getId() + ": ";
					// logic to handle the PQ update
					updateNeighborPriorityQ(fogNodeMsg);
				}

				ois.close();
				logPrefix =  "Fog " + fogNode.getId() + ": FOG-TCP LISTENER- ";
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (serverSock != null) {
				try {
					serverSock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void updateNeighborPriorityQ(FogNode fogNode) {
		PriorityQueue<NeighborNode> neighborQ = FogNode.getInstance().getNeighborQ();
		NeighborNode neighborInfo = new NeighborNode(fogNode.getIpAddress(), fogNode.getTcpPortNo(), fogNode.getId());
		neighborQ.remove(neighborInfo);
		neighborInfo.setCurrentProcessingTime(fogNode.getCurrentProcessingTime());
		neighborQ.add(neighborInfo);
	}

	public byte[] toByteArray(ByteBuffer byteBuffer) {
		byteBuffer.position(0);
		byteBuffer.limit(FogDefinitions.TCP_BUFFER_SIZE);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return bufArr;
	}

	/**
	 * UT test of Priority queue
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		NeighborNode n1 = new NeighborNode("127.0.0.1", 10, 1);
		n1.setCurrentProcessingTime(10000);
		NeighborNode n2 = new NeighborNode("127.0.0.1", 20, 2);
		n2.setCurrentProcessingTime(10000);
		FogNode n3 = FogNode.getInstance();
		n3.setIpAddress("127.0.0.1");
		n3.setTcpPortNo(4000);
		n3.setId(5);
//		n3.setCurrentProcessingTime(10000);
		PriorityQueue<NeighborNode> q = n3.getNeighborQ();

		q.add(n1);
		q.add(n2);
//		System.out.println(q.toString());
		TcpListener l = new TcpListener(null, null);
		l.updateNeighborPriorityQ(n3);
		System.out.println(q.toString());
		System.out.println(q.poll());
	}
}