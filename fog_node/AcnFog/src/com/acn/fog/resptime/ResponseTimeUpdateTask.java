package com.acn.fog.resptime;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.TimerTask;

import com.acn.fog.other.Helper;

/**
 * Class to periodically send Fog id, IP, currentProcessingTime
 * 
 * @author Vincy Shrine
 *
 */
public class ResponseTimeUpdateTask extends TimerTask {

	@Override
	public void run() {
		FogNode fogNode = FogNode.getInstance();
		String logPrefix = "Fog " + fogNode.getId() + ": FOG-RT PERIODIC SEND- ";
		List<NeighborNode> neighbors = fogNode.getNeighbors();
		for (NeighborNode neighbor : neighbors) {
			Socket socket = null;
			ObjectOutputStream out = null;
			try {
				Helper.sendTcpMessage(FogNode.getInstance(), neighbor.getIpAddress(), neighbor.getTcpPortNo(),
						logPrefix);
			} catch (IOException e) {
				System.err.println(logPrefix + " Sending update to " + neighbor.getId() + " failed: " + e.getMessage());
			} finally {
				if (out != null)
					try {
						out.close();
					} catch (IOException e) {
						System.err.println(logPrefix + " error: " + e.getMessage());
					}
				if (socket != null)
					try {
						socket.close();
					} catch (IOException e) {
						System.err.println(logPrefix + " error: " + e.getMessage());
					}
				logPrefix = "Fog " + fogNode.getId() + ": FOG-RT PERIODIC SEND- ";
			}
		}
	}
}