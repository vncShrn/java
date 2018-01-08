package com.aos.me.listener;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.packets.CsReqPacket;
import com.aos.me.packets.MeCompletePacket;
import com.aos.me.packets.MeStartPacket;
import com.aos.me.packets.PacketType;
import com.aos.me.packets.ParentPacket;
import com.aos.me.packets.ReadyPacket;
import com.aos.me.packets.RegisterPacket;
import com.aos.me.packets.SdPacket;
import com.aos.me.packets.TerminatePacket;
import com.aos.me.packets.TokenPacket;
import com.aos.me.process.Coordinator;
import com.aos.me.process.MeNode;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class ClientHandler implements Runnable {

	private Socket socket;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	Coordinator coordinator;
	MeNode process;
	String logPrefix;
	Helper helper;

	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	public ClientHandler(Socket socket) throws IOException {
		this.socket = socket;
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
		coordinator = Coordinator.getInstance();
		process = MeNode.getInstance();
		helper = new Helper();
	}

	@Override
	public void run() {
		String msgCount = "msg count= ";

		try {
			while (true) {
				logPrefix = " Process " + process.getId() + ": ";
				// + ": -Handler" + connectionCounter + "- " ;
				// System.out.println("Waiting for next readobj");

				Object readObject = ois.readObject();

				if (readObject != null) {

					// Coord receives REGISTER msg from all process
					if (readObject instanceof RegisterPacket && Definitions.AM_I_COORDINATOR) {

						String hostName = socket.getInetAddress().getCanonicalHostName();
						System.out.println(df.format(new Date()) + logPrefix + "Received REGISTER <= from " + hostName);
						RegisterPacket packet = coordinator.addToRegisteredNodes(hostName, socket.getPort());
						oos.writeObject(packet);
						disconnect();
						if (coordinator.isRegistrationComplete()) {
							// Sleep for sometime until all other process have
							// started their listeners
							Thread.sleep(100);
							coordinator.sendParentInfoToAllProcess(logPrefix);
						}
						break;

					} else if (readObject instanceof ParentPacket) {

						// Process receives Parent info from Coord
						ParentPacket packet = (ParentPacket) readObject;
						process.setParent(packet.getNode());
						process.setProcessInformation(packet.getAllProcessMap());
						System.out.println(df.format(new Date()) + logPrefix + "Received PARENT <= from COORD "
								+ packet.getNode().getProcessId());
						System.out.println(df.format(new Date()) + logPrefix + "Sending READY To COORDINATOR");
						helper.sendPacketToCoord(logPrefix, PacketType.READY, new ReadyPacket());
						// Send READY to the coordinator
						disconnect();
						break;

					} else if (readObject instanceof ReadyPacket && Definitions.AM_I_COORDINATOR) {

						// Coord should receive READY from all processes
						ReadyPacket packet = (ReadyPacket) readObject;
						System.out.println(
								df.format(new Date()) + logPrefix + "Received READY <= from " + packet.getSenderId());
						coordinator.addToReadyNodes(packet.getSenderId());
						disconnect();

						if (coordinator.isAllProcessReady()) {
							System.out.println(
									df.format(new Date()) + logPrefix + "All processes are ready to begin computation");

							Thread.sleep(1000);
							// Coord sends START ME msg to all its neighbors
							helper.sendPacketToAllProcess(logPrefix, PacketType.ME_START, new MeStartPacket());

							// Coordinator instantiates token object
							process.setToken(new TokenPacket());

							new CsRequestThread(logPrefix).start();

						}
						break;

					} else if (readObject instanceof MeStartPacket) {
						MeStartPacket packet = (MeStartPacket) readObject;
						System.out.println(df.format(new Date()) + logPrefix + "Received ME_START <= from "
								+ packet.getSenderId());

						new CsRequestThread(logPrefix).start();

					} else if (readObject instanceof SdPacket && Definitions.AM_I_COORDINATOR) {
						coordinator.addToSdPacketList((SdPacket) readObject);
					} else if (readObject instanceof CsReqPacket) {
						CsReqPacket packet = (CsReqPacket) readObject;
						int requestedBy = packet.getSenderId();

						System.out.println(df.format(new Date()) + logPrefix + "Received " + packet.getCsRequestString()
								+ "<= from " + requestedBy);

						// 1. Adds request to its request_q.
						process.getRequestQ().add(requestedBy);
						System.out.println(df.format(new Date()) + logPrefix + "req_Q = " + process.getRequestQ());

						/**
						 * Root node on receiving request: Sends token to the
						 * process from which request received. Sets holder to
						 * point to that process.
						 * 
						 */

						/**
						 * Dhwani Store the requests a process serves
						 */
						process.addToCsRequestMap(requestedBy, packet);

						if (process.isTokenWithMe()) {
							// This is root node
							if (!process.isCsExecutionOngoing()) {

								// Sends token to top of Q
								System.out.println(df.format(new Date()) + logPrefix + "Before sending TOKEN, req_Q = "
										+ process.getRequestQ());
								Integer topOfQ = process.getRequestQ().poll();
								if (topOfQ != null) {
									Map<Integer, CsReqPacket> csReqMap = process.getCsRequestMap();
									if (topOfQ == process.getId()) {
										// Enter CS
										process.getTokenQ().put(process.getToken());
										CsReqPacket servedCsReqPckt = csReqMap.get(process.getId());
										System.out.println(df.format(new Date()) + logPrefix + "Request: "
												+ packet.getCsRequestString() + " Messages: "
												+ servedCsReqPckt.getMessageCount());

										/**
										 * Dhwani, the process received the
										 * token, hence store it in successful
										 * completion map and remove from the
										 * serving map
										 */
										process.addToSuccessfulCsRequestMap(packet.getCsRequestString(),
												servedCsReqPckt.getMessageCount());
										process.removeFromCsRequestMap(process.getId());

									} else {

										// Send token to top of Q
										TokenPacket token = process.getToken();

										// if after polling, still the current
										// process has more items in Q
										if (process.getRequestQ().size() > 0) {

											token.setSenderToBeAddedToQ(true);

											/**
											 * Dhwani, send the information
											 * about next csRequest packet in
											 * token
											 */

											process.incrementMessageCount(process.getRequestQ().peek());
											token.setNextCsRequest(csReqMap.get(process.getRequestQ().peek()));

										}

										token.setSenderId(process.getId());
										token.setCsRequest(csReqMap.get(topOfQ));

										helper.sendPacketToNeighbor(logPrefix, PacketType.TOKEN, token, topOfQ);
										System.out.println(df.format(new Date()) + logPrefix
												+ "After sending TOKEN, req_Q = " + process.getRequestQ());

										// Change parent
										process.setParent(process.getProcessInformation().get(topOfQ));
										process.setTokenWithMe(false);
										process.setToken(null);
									}
								}
							}

						} else if (process.getRequestQ().size() == 1) {
							// It has already sent CS_REQ to parent
							// This is not a root node

							/**
							 * Sends request to neighbor towards root provided
							 * it has not forwarded another request to the
							 * neighbor.
							 */

							/**
							 * Dhwani, if token not with me, increment the
							 * message count
							 */

							packet.setMessageCount(packet.getMessageCount() + 1);
							packet.setSenderId(process.getId());
							helper.sendPacketToNeighbor(logPrefix, PacketType.CS_REQ, packet,
									process.getParent().getProcessId());

						}

					} else if (readObject instanceof TokenPacket) {

						TokenPacket token = (TokenPacket) readObject;
						System.out.println(
								df.format(new Date()) + logPrefix + "Received TOKEN <= from " + token.getSenderId());
						process.setTokenWithMe(true);
						process.setParent(null);
						/**
						 * Dhwani, Store information about the token for csReq
						 * and message count
						 */
						String csRequestString = token.getCsRequest().getCsRequestString();
						int messageCount = token.getCsRequest().getMessageCount();

						if (token.isSenderToBeAddedToQ()) {
							process.getRequestQ().add(token.getSenderId());
							/**
							 * Dhwani Send info about next csReq
							 */
							process.addToCsRequestMap(token.getSenderId(), token.getNextCsRequest());
							token.setSenderToBeAddedToQ(false);
						}
						System.out.println(df.format(new Date()) + logPrefix + "After receiving TOKEN, req_Q = "
								+ process.getRequestQ());

						Integer topOfQ = process.getRequestQ().poll();

						if (topOfQ != null) {
							if (topOfQ == process.getId()) {
								// Enter CS
								process.getTokenQ().put(token);
								System.out.println(df.format(new Date()) + logPrefix + csRequestString
										+ " received TOKEN in " + messageCount + " messages");

								/** Dhwani */
								process.addToSuccessfulCsRequestMap(csRequestString, messageCount);
								process.removeFromCsRequestMap(topOfQ);
							} else {
								Map<Integer, CsReqPacket> csReqMap = process.getCsRequestMap();
								CsReqPacket cs = csReqMap.get(topOfQ);
								cs.setMessageCount(messageCount);

								token.setCsRequest(cs);

								// Send token to top of Q
								if (process.getRequestQ().size() > 0) {
									token.setSenderToBeAddedToQ(true);
									process.incrementMessageCount(process.getRequestQ().peek());
									token.setNextCsRequest(csReqMap.get(process.getRequestQ().peek()));
								}
								token.setSenderId(process.getId());

								helper.sendPacketToNeighbor(logPrefix, PacketType.TOKEN, token, topOfQ);
								System.out.println(df.format(new Date()) + logPrefix + "After sending TOKEN, req_Q = "
										+ process.getRequestQ());

								// Change parent
								process.setParent(process.getProcessInformation().get(topOfQ));
								process.setTokenWithMe(false);
								process.setToken(null);
							}
						}

					}

					else if (readObject instanceof MeCompletePacket && Definitions.AM_I_COORDINATOR) {
						// Coord should receive ME_COMPLETE from all neighbors
						MeCompletePacket packet = (MeCompletePacket) readObject;
						System.out.println(df.format(new Date()) + logPrefix + "Received ME_COMPLETE <= from "
								+ packet.getSenderId());
						coordinator.addToCompleteNodes(packet.getSenderId());
						coordinator.addToAvgWaitTime(packet.getAvgWaitTime());
						coordinator.addToAvgMsgCount(packet.getAvgMsgCnt());
						msgCount += (int) packet.getAvgMsgCnt() + " ";

						if (coordinator.isAllProcessComplete()) {
							System.out.println(df.format(new Date()) + logPrefix + "All processes have completed ME");

							System.out.println(df.format(new Date()) + logPrefix + "Total wait time of all process: "
									+ coordinator.getAllProcessAvgWaitTime() + " " + Definitions.MAX_PROCESS);
							System.out.println(df.format(new Date()) + logPrefix + "******* Avg wait time = "
									+ coordinator.getAllProcessAvgWaitTime() / Definitions.MAX_PROCESS + " ms");
							System.out.println(df.format(new Date()) + logPrefix + msgCount + " avg msg count : "
									+ coordinator.getAllProcessAvgMsgCnt() + " total process = "
									+ Definitions.MAX_PROCESS);
							System.out.println(df.format(new Date()) + logPrefix + "******* Avg msg count = "
									+ (int) Math.ceil(coordinator.getAllProcessAvgMsgCnt() / Definitions.MAX_PROCESS));

							computeSD(logPrefix);

							// Coord sends TERMINATE msg to all its neighbors
							helper.sendPacketToAllProcess(logPrefix, PacketType.TERMINATE, new TerminatePacket());
							System.out.println(df.format(new Date()) + logPrefix + "Going to die...");
							ServerListener.getInstance().getExecutor().shutdownNow();
							System.exit(0);
						}
					} else if (readObject instanceof TerminatePacket) {
						System.out.println(df.format(new Date()) + logPrefix + "Received TERMINATE <= from "
								+ ((TerminatePacket) readObject).getSenderId());
						System.out.println(df.format(new Date()) + logPrefix + "Going to die...");
						ServerListener.getInstance().getExecutor().shutdownNow();
						System.exit(0);
					}
				}
			}
		} catch (EOFException e) {
			// System.out.println(
			// logPrefix + "Error: EOF: " + e.getMessage() + " from: " +
			// socket.getInetAddress().getHostName());
		} catch (IOException | InterruptedException e) {
			System.out.println(df.format(new Date()) + logPrefix + "Error: IO: " + e.getMessage() + " from: "
					+ socket.getInetAddress().getHostName());
		} catch (ClassNotFoundException e) {
			System.out.println(df.format(new Date()) + logPrefix + "Error: CNF: " + e.getMessage() + " from: "
					+ socket.getInetAddress().getHostName());
		}  catch (Exception e) {
			System.out.println(df.format(new Date()) + logPrefix + "Error: Exception: " + e.getMessage() + " from: "
					+ socket.getInetAddress().getHostName());
		} finally {
			System.out.println(
					df.format(new Date()) + logPrefix + "Completed handling: " + socket.getInetAddress().getHostName());
		}

	}

	public void disconnect() {
		try {
			if (oos != null) {
				oos.flush();
				oos.close();
			}
			if (ois != null)
				ois.close();
			if (socket != null)
				socket.close();
			// System.out.println(df.format(new Date()) + logPrefix + "Socket
			// Closed: " + socket.getInetAddress().getHostName() + " port:
			// "+socket.getPort());
		} catch (IOException e) {
			System.out.println(df.format(new Date()) + logPrefix + "Error: Socket Close: " + e.getMessage() + " from: "
					+ socket.getInetAddress().getHostName());
		}
	}

	private void computeSD(String logPrefix) {
		try {
			int lineIndex = 0;
			long timeSum = 0;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			long prevExitTime = 0, entryTime = 0;
			for (SdPacket packet : Coordinator.getInstance().getSdPacketList())
				System.out.println(packet);
			System.out.print(df.format(new Date()) + logPrefix + "SD = ");

			for (SdPacket packet : Coordinator.getInstance().getSdPacketList()) {
				lineIndex = lineIndex + 1;
				Date entryDate = sdf.parse("1970-01-01 " + packet.getEntryTime());
				Date exitDate = sdf.parse("1970-01-01 " + packet.getExitTime());
				long exitTime = exitDate.getTime();

				entryTime = entryDate.getTime();

				if (lineIndex == 1) {
					prevExitTime = exitTime;
					continue;
				}

				long sd = entryTime - prevExitTime;
				if (sd < 0)
					sd = 0;
				timeSum += sd;

				System.out.print(sd + " ");
				prevExitTime = exitTime;

			}
			System.out.println();
			int totalCSRequests = lineIndex;
			long avgSD = timeSum / totalCSRequests;
			System.out.println(df.format(new Date()) + logPrefix + "******* Avg SD = " + avgSD
					+ " ms for total CS req#: " + totalCSRequests);
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}
}
