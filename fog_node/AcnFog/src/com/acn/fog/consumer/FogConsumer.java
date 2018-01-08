package com.acn.fog.consumer;

import java.util.concurrent.ArrayBlockingQueue;

import com.acn.fog.listener.Packet;
import com.acn.fog.other.Helper;
import com.acn.fog.resptime.FogNode;

public class FogConsumer implements Runnable {

	private ArrayBlockingQueue<Packet> queue = null;

	public FogConsumer(ArrayBlockingQueue<Packet> fogQueue) {
		queue = fogQueue;
	}

	@Override
	public void run() {

		FogNode fogNode = FogNode.getInstance();
		String logPrefix = "Fog " + fogNode.getId() + ": FOG-CONSUMER- ";
		System.out.println(logPrefix + "Consumer thread is started...Awaiting packets..");
		while (true) {
			Packet packet = null;
			try {
				packet = queue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// sleep for response time duration of packet
			try {
				System.out.println(logPrefix + "Serving packet: " + packet.getSeqNo());
				Thread.sleep(packet.getProcessingTime());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			fogNode.subtractFromCurrentProcessingTime(packet.getProcessingTime());

			// UDP reply to IOT
			System.out.println(logPrefix + "Serving packet: " + packet.getSeqNo() + " completed");
			packet.addLog("Served by fog;");
			Helper.sendUdpResponse((packet.toString()).getBytes(), packet.getIotIpAddress(), packet.getIotPortNo(),
					logPrefix);
		}
	}

}
