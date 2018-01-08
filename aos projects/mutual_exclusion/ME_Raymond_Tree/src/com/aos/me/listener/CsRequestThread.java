package com.aos.me.listener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map.Entry;
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
 * @author Dhwani Raval
 */
public class CsRequestThread extends Thread {

	private String logPrefix;
	private TokenPacket token = null;
	private Helper helper = new Helper();

	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	public CsRequestThread(String log) {
		this.logPrefix = log;
	}

	public void run() {
		/** ME - CS Request starts */
		MeNode process = MeNode.getInstance();

		int pid = process.getId();
		long waitTimeSum = 0;

		int maxRequests = ThreadLocalRandom.current().nextInt(Definitions.MIN_CS_REQ_COUNT,
				Definitions.MAX_CS_REQ_COUNT + 1);

		System.out.println(df.format(new Date()) + logPrefix + "Going to make #" + maxRequests + " CS requests");

		for (int i = 0; i < maxRequests; i++) {

			try {
				long sleepTime = ThreadLocalRandom.current().nextLong(Definitions.T1, Definitions.T2);
				try {
					System.out.println(df.format(new Date()) + logPrefix + "Going to sleep : " + sleepTime
							+ " ms .. before req#" + (i + 1));
					Thread.sleep(sleepTime);
					System.out.println(df.format(new Date()) + logPrefix + "Woke up after : " + sleepTime
							+ " ms .. before req#" + (i + 1));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				Date csTokenReqTime = new Date();
				/**
				 * 
				 * Pi wishing to enter critical section sends REQUEST to
				 * neighbor indicated by holder provided: Pi does not hold the
				 * token, and Pi’s request_q is empty.
				 * 
				 */

				CsReqPacket csRequest = new CsReqPacket();
				csRequest.setSenderId(process.getId());
				csRequest.setCsRequestString(pid + PacketType.CS_REQ.toString() + (i + 1));

				// add to req q
				process.getRequestQ().add(pid);

				System.out.println();
				System.out.println(
						df.format(new Date()) + logPrefix + "New CS Request : " + csRequest.getCsRequestString());
				System.out.println();

				/*
				 * if (!process.isTokenWithMe() &&
				 * process.getRequestQ().isEmpty())
				 */
				if (!process.isTokenWithMe() && process.getRequestQ().size() == 1) {
					// send req to parent
					csRequest.setMessageCount(csRequest.getMessageCount() + 1);

					helper.sendPacketToNeighbor(logPrefix, PacketType.CS_REQ, csRequest,
							process.getParent().getProcessId());

				}

				/**
				 * Dhwani Store the requests a process serves
				 */
				process.addToCsRequestMap(pid, csRequest);
				System.out.println(df.format(new Date()) + logPrefix + "New CS_REQ, req_Q =  " + process.getRequestQ());

				if (process.isTokenWithMe() && process.getRequestQ().peek() == pid) {
					process.getRequestQ().poll();
					token = process.getToken();
					/**
					 * Dhwani, the process received the token, hence store it in
					 * successful completion map and remove from the serving map
					 */
					CsReqPacket servedPckt = process.getCsRequestMap().get(pid);
					System.out.println("\n" + df.format(new Date()) + logPrefix + csRequest.getCsRequestString()
							+ " received TOKEN in " + servedPckt.getMessageCount() + " messages\n");

					process.addToSuccessfulCsRequestMap(csRequest.getCsRequestString(), servedPckt.getMessageCount());
					process.removeFromCsRequestMap(pid);

				} else {
					// Wait for token

					System.out.println("\n" + df.format(new Date()) + logPrefix + "Waiting for TOKEN...\n");

					try {
						token = MeNode.getInstance().getTokenQ().take();
						MeNode.getInstance().setToken(token);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				process.setCsExecutionOngoing(true);

				Date csTokenRecvdTime = new Date();

				long waitTime = (csTokenRecvdTime.getTime() - csTokenReqTime.getTime());
				waitTimeSum += waitTime;

				System.out.println(df.format(new Date()) + logPrefix + "TOKEN recvd at: " + df.format(csTokenRecvdTime)
						+ " requested at: " + df.format(csTokenReqTime) + " diff: " + waitTime + " ms");

				// Execute CS - Begin

				try {
					System.out.println(df.format(new Date()) + logPrefix + "Entering CS... for req#" + (i + 1));
					Thread.sleep(Definitions.T3);
					System.out.println(df.format(new Date()) + logPrefix + "Exiting CS...  for req#" + (i + 1));
				} catch (InterruptedException e) {
					System.err.println(df.format(new Date()) + logPrefix + "CS sleep interrupted: for req#" + (i + 1)
							+ e.getMessage());
				}

				// Execute CS - End

				System.out
						.println(df.format(new Date()) + logPrefix + "After CS Exit, req_Q = " + process.getRequestQ());
				Integer topOfQ = process.getRequestQ().poll();

				if (topOfQ != null) {
					// Send token to top of Q

					/**
					 * Dhwani, Send the csRequest packet info in token
					 */
					token.setCsRequest(process.getCsRequestMap().get(topOfQ));

					System.out.println();
					System.out.println(df.format(new Date()) + logPrefix + "Next CS Req is "
							+ token.getCsRequest().getCsRequestString());
					System.out.println();

					if (process.getRequestQ().size() > 0) {
						token.setSenderToBeAddedToQ(true);
						/**
						 * Dhwani, send the information about next csRequest
						 * packet in token
						 */
						process.incrementMessageCount(process.getRequestQ().peek());
						token.setNextCsRequest(process.getCsRequestMap().get(process.getRequestQ().peek()));

					}
					token.setSenderId(pid);
					helper.sendPacketToNeighbor(logPrefix, PacketType.TOKEN, token, topOfQ);
					// Change parent

					/**
					 * Dhwani, remove the information about csRequest for which
					 * the token is passed on
					 */
					process.removeFromCsRequestMap(topOfQ);

					// Change parent
					process.setParent(process.getProcessInformation().get(topOfQ));
					process.setTokenWithMe(false);
					process.setToken(null);

				}
				process.setCsExecutionOngoing(false);
				String csExitTime = df.format(new Date());
				helper.sendPacketToNeighbor(logPrefix, PacketType.SD,
						new SdPacket(i + 1, df.format(csTokenRecvdTime), csExitTime), 1);

			} catch (Exception e) {
				e.printStackTrace();
				System.err
						.println(df.format(new Date()) + logPrefix + " for cs req# " + (i + 1) + " " + e.getMessage());
				System.out
						.println(df.format(new Date()) + logPrefix + " for cs req# " + (i + 1) + " " + e.getMessage());
			}

		}
		// Once CS request is done for [20-40] times, send ME_COMPLETE to coord
		/**
		 * Dhwani, display number of Messages
		 * 
		 * A for loop to display the number of messages stored in the successful
		 * completion of CS Request Map
		 */

		System.out.println("\n" + df.format(new Date()) + logPrefix + "Request --> Message Count");

		int msgReqSum = 0;
		for (Entry<String, Integer> entry : process.getSuccessfulCsRequestMap().entrySet()) {
			int cnt = entry.getValue();
			System.out.println(df.format(new Date()) + logPrefix + entry.getKey() + " --> " + cnt);
			msgReqSum += cnt;
		}
		MeCompletePacket packet = new MeCompletePacket();

		packet.setAvgWaitTime(waitTimeSum / maxRequests);
		System.out.println(df.format(new Date()) + logPrefix + "avgWaitTime: " + packet.getAvgWaitTime());

		packet.setAvgMsgCnt(Math.ceil(msgReqSum / maxRequests));
		System.out.println(df.format(new Date()) + logPrefix + "total csreq= " + msgReqSum + " maxReq: " + maxRequests
				+ " avg= " + String.format("%.2f", packet.getAvgMsgCnt()));

		helper.sendPacketToNeighbor(logPrefix, PacketType.ME_COMPLETE, packet, 1);
	}

}
