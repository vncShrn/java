package com.aos.me.poll;

import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimerTask;

import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.packets.PacketType;
import com.aos.me.packets.ReadyPacket;
import com.aos.me.process.MeNode;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class DidAllNeighborsHandshake extends TimerTask {

	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	public void run() {
		MeNode process = MeNode.getInstance();
		String logPrefix = " Process " + process.getId() +": ";
		if (process.isReceivedAllHello()) {
			// Send READY msg to Coord
			System.out.println(df.format(new Date())+logPrefix+"Received HELLO from all neighbors");
			Socket coordSocket = null;
			try {
				coordSocket = new Socket(Definitions.COORDINATOR_HOSTNAME, Definitions.COORD_PORT);
			} catch (IOException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
				return;
			}
			(new Helper()).sendTcpMessage(new ReadyPacket(process.getId()), coordSocket,
					logPrefix+"Send " + PacketType.READY + " to => " + 1);
			Thread.currentThread().interrupt();
			this.cancel();
			return;
		}

	}
}
