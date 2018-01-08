package com.aos.me.listener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.packets.CsReqPacket;
import com.aos.me.packets.MeCompletePacket;
import com.aos.me.packets.PacketType;
import com.aos.me.packets.SdPacket;
import com.aos.me.packets.TokenPacket;
import com.aos.me.process.MeNode;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class CsRequestThread extends Thread {

	private String logPrefix;
	private TokenPacket token = null;
	private Helper helper = new Helper();

	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	public CsRequestThread(String log) {
		this.logPrefix = log + " ";
	}

	public void run() {
		/** ME - CS Request starts */
		MeNode process = MeNode.getInstance();

		int pid = process.getId();

		int maxRequests = ThreadLocalRandom.current().nextInt(Definitions.MIN_CS_REQ_COUNT,
				Definitions.MAX_CS_REQ_COUNT + 1);

		long waitTimeSum = 0;
		
		System.out.println(df.format(new Date()) + logPrefix + "Going to make #" + maxRequests + " CS requests");

		for (int i = 0; i < maxRequests; i++) {

			long sleepTime = ThreadLocalRandom.current().nextLong(Definitions.T1, Definitions.T2);
			try {
				System.out.println(df.format(new Date()) + logPrefix + "Going to sleep : " + sleepTime / 1000
						+ " secs .. before req#" + (i + 1));
				Thread.sleep(sleepTime);
				System.out.println(df.format(new Date()) + logPrefix + "Woke up after : " + sleepTime / 1000
						+ " secs .. before req#" + (i + 1));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			int RNi;

			Date csTokenReqTime = new Date();

			// Request for token
			if (process.isTokenWithMe()) {
				// token is with me
				process.setCsExecutionOngoing(true);
				System.out.println(df.format(new Date()) + logPrefix + "Token is with me for req: " + (i + 1));
				token = process.getToken();

				if (i == 0) {
					// Increments RNi[i]
					RNi = process.incrementRN(pid);
				}

			} else {
				System.out.println(df.format(new Date()) + logPrefix + "Token not with me for req: " + (i + 1));

				// Increments RNi[i]
				RNi = process.incrementRN(pid);

				// Sends REQUEST(i, RNi[i]) to all processes
				helper.sendPacketToAllNeighbors(logPrefix, PacketType.CS_REQ, new CsReqPacket(RNi));

				// Wait for token
				try {
					System.out.println(df.format(new Date()) + logPrefix + "Waiting for token... for req "+ (i + 1)+"\n");
					token = process.getTokenQ().take();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			process.setCsExecutionOngoing(true);

			Date csTokenRecvdTime = new Date();

			long waitTime = (csTokenRecvdTime.getTime() - csTokenReqTime.getTime());
			waitTimeSum +=waitTime;

			System.out.println(df.format(new Date()) + logPrefix + "TOKEN recvd@ " + df.format(csTokenRecvdTime)
					+ " req@ " + df.format(csTokenReqTime) + " wait time: " + waitTime + " ms");

			try {
				System.out.println("\n"+df.format(csTokenRecvdTime) + logPrefix + "Entering CS... for req#" + (i + 1));
				Thread.sleep(Definitions.T3);
				System.out.println(df.format(new Date()) + logPrefix + "Exiting CS...  for req#" + (i + 1)+"\n");
			} catch (InterruptedException e) {
				System.err.println(df.format(new Date()) + logPrefix + "CS sleep interrupted: for req#" + (i + 1)
						+ e.getMessage());
			}

			// Execute CS - End

			/** 1. LN[i] = RNi[i] */
			token.setLN(pid, process.getRN(pid));

			/**
			 * 2. Check with RN and add to token Q
			 */
			List<Integer> RN = process.getRNList();
			List<Integer> LN = token.getLNList();
			for (int index = 0; index < RN.size(); index++) {
				if (index + 1 != pid) {
					// for all Pj other than Pi
					if (!token.getTokenQ().contains(index + 1) && (RN.get(index) == LN.get(index) + 1)) {
						// for all Pj not in tokenQ and satisfies condition
						token.addToTokenQ(index + 1);
						System.out
								.println(df.format(new Date()) + logPrefix + "Added missed CS_REQ from: " + (index + 1));
					}
				}
			}

			/**
			 * 3. If token queue is non-empty: Dequeue process-id at the head of
			 * the queue and forward the token to the corresponding process
			 */
			Integer next = null;
			System.out.println(df.format(new Date()) + logPrefix + "tokenQ: " + token.getTokenQ());
			if ((next = token.getTokenQ().poll()) != null) {
				token.setSenderId(pid);
				helper.sendPacketToNeighbor(logPrefix, PacketType.TOKEN, token, next);
				process.setTokenWithMe(false);
				process.setToken(null);
			}
			String csExitTime = df.format(new Date());
			process.setCsExecutionOngoing(false);
			helper.sendPacketToNeighbor(logPrefix, PacketType.SD,
					new SdPacket(i + 1, df.format(csTokenRecvdTime), csExitTime), 1);

		}
		// Once CS request is done for [20-40] times, send ME_COMPLETE to coord
		MeCompletePacket packet = new MeCompletePacket();
		packet.setAvgWaitTime(waitTimeSum/maxRequests);
		packet.setAvgMsgCnt(process.getTotalCsReqMsgsSent() / maxRequests);
		System.out.println(df.format(new Date()) + logPrefix + "total csreq= " + process.getTotalCsReqMsgsSent()
				+ " maxReq: " + maxRequests + " avg= " + String.format("%.2f", packet.getAvgMsgCnt()));

		helper.sendPacketToNeighbor(logPrefix, PacketType.ME_COMPLETE, packet, 1);
	}

}
