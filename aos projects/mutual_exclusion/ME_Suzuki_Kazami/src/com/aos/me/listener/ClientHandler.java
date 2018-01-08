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
import java.util.TimeZone;

import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.packets.CsReqPacket;
import com.aos.me.packets.HelloPacket;
import com.aos.me.packets.MeCompletePacket;
import com.aos.me.packets.MeStartPacket;
import com.aos.me.packets.NeighborsPacket;
import com.aos.me.packets.PacketType;
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
	static private int connectionCounter;
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
		connectionCounter++;
	}

	@Override
	public void run() {

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
							Thread.sleep(20);
							coordinator.sendNeighborInfoToAllProcess(logPrefix);
						}
						break;

					} else if (readObject instanceof NeighborsPacket) {

						// Process receives Neighbor info from Coord
						NeighborsPacket packet = (NeighborsPacket) readObject;
						process.setNeighbors(packet.getNode().getNeighbors());
						System.out.println(df.format(new Date()) + logPrefix + "Received NEIGHBOR <= from "
								+ process.getNeighbors());
						// Send HELLO to all neighbors
						helper.sendPacketToAllNeighbors(logPrefix, PacketType.HELLO, new HelloPacket());
						disconnect();
						break;

					} else if (readObject instanceof HelloPacket) {

						// Process receives HELLO from neighbors
						HelloPacket packet = (HelloPacket) readObject;
						System.out.println(
								df.format(new Date()) + logPrefix + "Received HELLO <= from " + packet.getSenderId());
						process.addToHelloNodes(packet.getSenderId());
						disconnect();
						break;

					} else if (readObject instanceof ReadyPacket && Definitions.AM_I_COORDINATOR) {

						// Coord should receive READY from all neighbors
						ReadyPacket packet = (ReadyPacket) readObject;
						System.out.println(
								df.format(new Date()) + logPrefix + "Received READY <= from " + packet.getSenderId());
						coordinator.addToReadyNodes(packet.getSenderId());
						if (coordinator.isAllProcessReady()) {
							System.out.println(
									df.format(new Date()) + logPrefix + "All processes are ready to begin computation");

							// Coord sends START ME msg to all its neighbors
							helper.sendPacketToAllNeighbors(logPrefix, PacketType.ME_START, new MeStartPacket());

							// Coordinator instantiates token object
							process.setToken(new TokenPacket());

							// Coordinator starts cs requests
							new CsRequestThread(logPrefix).start();

						}
						disconnect();
						break;

					} else if (readObject instanceof MeStartPacket) {
						MeStartPacket packet = (MeStartPacket) readObject;
						System.out.println(df.format(new Date()) + logPrefix + "Received START_ME <= from "
								+ packet.getSenderId());

						new CsRequestThread(logPrefix).start();

					} else if (readObject instanceof TokenPacket) {

						TokenPacket token = (TokenPacket) readObject;
						System.out.println(
								df.format(new Date()) + logPrefix + "Received TOKEN <= from " + token.getSenderId());
						System.out.println(df.format(new Date()) + logPrefix + "tokenQ=" + token.getTokenQ());
						process.setTokenWithMe(true);
						process.setToken(token);
						process.getTokenQ().put(token);
					} else if (readObject instanceof SdPacket && Definitions.AM_I_COORDINATOR) {
						coordinator.addToSdPacketList((SdPacket) readObject);
					}

					else if (readObject instanceof CsReqPacket) {
						CsReqPacket packet = (CsReqPacket) readObject;
						int requestedBy = packet.getSenderId();

						System.out
								.println(df.format(new Date()) + logPrefix + "Received CS_REQ <= from " + requestedBy);

						// 1. RNj[i] = max(RNj[i], received RNi[i])
						int oldRnJ = process.getRN(requestedBy);
						process.setRN(requestedBy, Integer.max(oldRnJ, packet.getRNi()));

						// 2. Sends idle token (if available) provided RNj[i] =
						// LN[i]+1
						// (otherwise the request is stale)
						if (process.isTokenWithMe()) {
							// Token is with me
							TokenPacket token = process.getToken();

							if (process.getRN(requestedBy) == token.getLN(requestedBy) + 1) {

								if (!process.isCsExecutionOngoing()) {

									// Token is available
									System.out.println(df.format(new Date()) + logPrefix
											+ "Token is available. Sending token to: " + requestedBy);
									System.out
											.println(df.format(new Date()) + logPrefix + "tokenQ=" + token.getTokenQ());
									token.setSenderId(process.getId());
									helper.sendPacketToNeighbor(logPrefix, PacketType.TOKEN, token, requestedBy);
									process.setTokenWithMe(false);
									process.setToken(null);

								} else {

									// Token is busy, add to end of Q
									token.getTokenQ().add(requestedBy);
									System.out.println(df.format(new Date()) + logPrefix + "is in CS. Hence adding pid "
											+ requestedBy + " to tokenQ: " + token.getTokenQ());
								}
							} else {

								System.out.println(df.format(new Date()) + logPrefix + "RN(" + requestedBy + "): "
										+ process.getRN(requestedBy) + " LN(" + requestedBy + "): "
										+ token.getLN(requestedBy));
								System.out.println(df.format(new Date()) + logPrefix + "Stale token request by: "
										+ requestedBy + " It stands blocked..");
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
						if (coordinator.isAllProcessComplete()) {
							System.out.println(df.format(new Date()) + logPrefix + "All processes have completed ME");

							System.out.println(df.format(new Date()) + logPrefix + "Total wait time of all process: "
									+ coordinator.getAllProcessAvgWaitTime() + " " + Definitions.MAX_PROCESS);
							System.out.println(df.format(new Date()) + logPrefix + "******* Avg wait time : "
									+ coordinator.getAllProcessAvgWaitTime() / Definitions.MAX_PROCESS + " ms");
							System.out.println(df.format(new Date()) + logPrefix + "Total avg msg count : "
									+ coordinator.getAllProcessAvgMsgCnt() + " total process = "
									+ Definitions.MAX_PROCESS);
							System.out.println(df.format(new Date()) + logPrefix + "******* Avg msg count : "
									+ Math.ceil(coordinator.getAllProcessAvgMsgCnt() / Definitions.MAX_PROCESS));

							computeSD(logPrefix);

							// Coord sends TERMINATE msg to all its neighbors
							helper.sendPacketToAllNeighbors(logPrefix, PacketType.TERMINATE, new TerminatePacket());
							ServerListener.getInstance().getExecutor().shutdownNow();
							System.out.println("Going to die...");
							System.exit(0);
							break;
						}
					}

					else if (readObject instanceof TerminatePacket) {
						System.out.println(df.format(new Date()) + logPrefix + "Received TERMINATE <= from "
								+ ((TerminatePacket) readObject).getSenderId());
						ServerListener.getInstance().getExecutor().shutdownNow();
						System.out.println("Going to die...");
						System.exit(0);
						break;
					}

				}
			}
		} catch (EOFException e) {
			System.out.println(
					logPrefix + "Error: EOF: " + e.getMessage() + " from: " + socket.getInetAddress().getHostName());
		} catch (IOException | InterruptedException e) {
			System.out.println(
					logPrefix + "Error: IO: " + e.getMessage() + " from: " + socket.getInetAddress().getHostName());
		} catch (ClassNotFoundException e) {
			System.out.println(
					logPrefix + "Error: CNF: " + e.getMessage() + " from: " + socket.getInetAddress().getHostName());
		} catch (Exception e) {
			System.out.println(logPrefix + "Error: Exception: " + e.getMessage() + " from: "
					+ socket.getInetAddress().getHostName());
		} finally {
			System.out.println(
					df.format(new Date()) + logPrefix + "Completed handling: " + socket.getInetAddress().getHostName());
			connectionCounter--;
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
