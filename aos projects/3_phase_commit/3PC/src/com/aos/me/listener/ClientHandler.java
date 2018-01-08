package com.aos.me.listener;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.other.Node;
import com.aos.me.other.PacketType;
import com.aos.me.other.StateT;
import com.aos.me.packets.AbortPacket;
import com.aos.me.packets.AckPacket;
import com.aos.me.packets.AgreedPacket;
import com.aos.me.packets.CommitPacket;
import com.aos.me.packets.CommitRequestPacket;
import com.aos.me.packets.HelloPacket;
import com.aos.me.packets.Packet;
import com.aos.me.packets.PreparePacket;
import com.aos.me.packets.TerminatePacket;
import com.aos.me.process.Coordinator;
import com.aos.me.process.MeNode;
import com.aos.me.process.Streams;

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
	StateT failAtState = null;
	boolean isReplyAbort = false;
	private StateT currentState;
	CommitRequestPacket commitPacket;
	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	public ClientHandler(Socket socket) throws IOException {
		this.socket = socket;
		// socket.setSoTimeout(Definitions.SOCKET_TIMEOUT);
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
		coordinator = Coordinator.getInstance();
		process = MeNode.getInstance();
		helper = new Helper();
	}

	@Override
	public void run() {
		
		boolean isCommitThreadAlive = false;

		try {
			while (true) {
				logPrefix = "Process " + process.getId() + ": ";
				// + ": -Handler" + connectionCounter + "- " ;
				// System.out.println("Waiting for next readobj");

				Object readObject = ois.readObject();

				if (readObject != null) {

					// Coord receives HELLO msg from all process
					if (readObject instanceof HelloPacket && Definitions.AM_I_COORDINATOR) {

						// Coord should receive READY from all neighbors
						HelloPacket packet = (HelloPacket) readObject;
						System.out.println(logPrefix + "Received HELLO <= from " + packet.getSenderId());
						coordinator.addToReadyNodes(packet.getSenderId());
						if (coordinator.isAllProcessReady() && !isCommitThreadAlive) {
							System.out.println(logPrefix + "All processes are ready to begin computation");

							// Sleep for sometime before 3PC can be started
							System.out
									.println(logPrefix + "I am COORDINATOR... I am waiting for COMMIT_REQ transaction");

							Thread.sleep(20);
							Thread commitRequestThread = new Thread(
									new CommitRequestThread("Process " + process.getId() + ":"));
							commitRequestThread.start();
							isCommitThreadAlive = true;
						}

					} else if (readObject instanceof CommitRequestPacket) {
						commitPacket = (CommitRequestPacket) readObject;
						logPrefix += "T=" + commitPacket.getTransactionId() + " : ";

						if (Definitions.AM_I_COORDINATOR) {

							if (commitPacket.isCoord()) {
								failAtState = commitPacket.getFailAtState();
							} else {
								failAtState = null;
							}

							System.out.println(logPrefix + "COORD Received COMMIT_REQ: " + commitPacket);

							currentState = StateT.REQ_RECEIVED;
							if (currentState != failAtState) {

								boolean isAllAgreedReceived = sendCommitReqToAllNeighbors(logPrefix,
										PacketType.COMMIT_REQ, commitPacket);
								coordinator.resetAgreedSet();

								currentState = StateT.WAIT;

								if (currentState == failAtState) {
									createFailureFile(currentState, commitPacket);
									System.out.println(logPrefix
											+ "COORD fails before sending PREPARE, cohorts should timeout waiting for PREPARE and ABORT");
									System.out.println(logPrefix + "COORD on recovery ABORTS transaction");
									die();
								}

								// Move to phase 2
								// COORD sends PREPARE to all
								if (isAllAgreedReceived) {
									currentState = StateT.PREPARE;

									boolean isAllPrepareAckReceived = sendPrepareReqToAllNeighbors(logPrefix,
											PacketType.PREPARE, new PreparePacket());
									coordinator.resetAckSet();

									if (isAllPrepareAckReceived) {
										// Move to phase 3
										System.out.println(logPrefix
												+ "ACK received for all PREPARE, Transaction has to be commited");

										if (failAtState != currentState) {
											currentState = StateT.COMMIT;
											sendCommitToAllNeighbors(logPrefix, PacketType.COMMIT, new CommitPacket());
											writeToTransactionFile(logPrefix, currentState, commitPacket);
										} else {
											System.out.println(logPrefix
													+ "COORD fails before sending COMMIT, cohorts should timeout, go ahead and COMMIT");
											createFailureFile(currentState, commitPacket);
											die();
										}
									} else {
										currentState = StateT.ABORT;
										// Someone has not responded with ACK
										// So send ABORT to all cohorts
										System.out.println(
												logPrefix + "Transaction aborted, as someone did not ACK to PREPARE");
										helper.sendPacketToAllNeighbors(logPrefix, PacketType.ABORT, new AbortPacket());
										writeToTransactionFile(logPrefix, currentState, commitPacket);
									}

								} else {
									currentState = StateT.ABORT;
									// Someone has not responded with Agreed
									// message
									// So send ABORT to all cohorts
									System.out.println(
											logPrefix + "Transaction aborted, as someone did not agree to COMMIT_REQ");
									helper.sendPacketToAllNeighbors(logPrefix, PacketType.ABORT, new AbortPacket());
									writeToTransactionFile(logPrefix, currentState, commitPacket);
								}

							} else {
								if (failAtState == currentState) {
									createFailureFile(currentState, commitPacket);
									System.out.println(logPrefix + "COORD fails at state: " + failAtState);
									die();
								} else {
									currentState = StateT.ABORT;
									System.out.println(logPrefix + "COORD timesout at state: " + isReplyAbort);
									writeToTransactionFile(logPrefix, currentState, commitPacket);
								}
							}
							logPrefix = " Process " + process.getId() + ": ";
							process.getTokenQ().put(new String());
						} else {
							System.out
									.println("*\n*\n*\n" + logPrefix + "Received COMMIT_REQ from <= 1" + commitPacket);

							currentState = StateT.REQ_RECEIVED;

							if (!commitPacket.isCoord() && commitPacket.getCohortId() == process.getId()) {
								failAtState = commitPacket.getFailAtState();
								isReplyAbort = commitPacket.isReplyAbort();
							} else {
								failAtState = null;
							}

							if (!isReplyAbort) {

								oos.writeObject(new AgreedPacket());
								oos.flush();
								System.out.println(logPrefix + "Send AGREED to => 1");
								currentState = StateT.WAIT;

							} else {
								oos.writeObject(new AbortPacket());
								oos.flush();
								System.out.println(logPrefix + "Send ABORT to => 1");
							}
							if (failAtState == currentState) {
								createFailureFile(currentState, commitPacket);
								die();
							}
						}

					} else if (readObject instanceof PreparePacket) {
						System.out.println(logPrefix + "Received PREPARE from <= 1");
						currentState = StateT.PREPARE;

						oos.writeObject(new AckPacket());
						oos.flush();
						System.out.println(logPrefix + "Send ACK to => 1");

						// cohort fails while waiting for COMMIT from coord
						if (failAtState == currentState) {
							createFailureFile(currentState, commitPacket);
							die();
						}

					} else if (readObject instanceof CommitPacket) {

						System.out.println(logPrefix + "Received COMMIT from <= 1");

						currentState = StateT.COMMIT;

						if (failAtState != currentState)
							writeToTransactionFile(logPrefix, currentState, commitPacket);
						else {
							createFailureFile(currentState, commitPacket);
							die();
						}

					} else if (readObject instanceof AbortPacket) {

						System.out.println(logPrefix + "Received ABORT from <= 1");

						currentState = StateT.ABORT;

						if (failAtState != currentState)
							writeToTransactionFile(logPrefix, currentState, commitPacket);
						else {
							createFailureFile(currentState, commitPacket);
							die();
						}

					} else if (readObject instanceof TerminatePacket) {

						System.out.println(logPrefix + "Received TERMINATE from <= 1");
						die();
					}

				}
			}
		} catch (EOFException | SocketException ex) {
			if (commitPacket != null) {
				if (!Definitions.AM_I_COORDINATOR) {
					if (currentState == StateT.PREPARE) {
						// if i am cohort, commit the transaction on timeout of
						// COMMIT from COORD
						System.out
								.println(logPrefix + "Cohort timed out at state p, waiting for COMMIT msg from COORD");
						currentState = StateT.COMMIT;
						writeToTransactionFile(logPrefix, currentState, commitPacket);
					} else {
						System.out.println(logPrefix + "Cohort timed out at state: " + currentState);
						currentState = StateT.ABORT;
						writeToTransactionFile(logPrefix, currentState, commitPacket);
					}
				} else
					System.out
							.println(logPrefix + "COORD timed out at state: " + currentState + " : " + ex.getMessage());
			}
		} catch (InterruptedException e) {
			System.out.println(logPrefix + "Error: Interrupted: " + e.getMessage() + " from: "
					+ socket.getInetAddress().getHostName());
		} catch (ClassNotFoundException e) {
			System.out.println(
					logPrefix + "Error: CNF: " + e.getMessage() + " from: " + socket.getInetAddress().getHostName());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(logPrefix + "Error: Exception: " + e.getMessage() + " during: "
					+ socket.getInetAddress().getHostName());
		} finally {
			System.out.println(logPrefix + "Completed handling: " + socket.getInetAddress().getHostName() + " : "
					+ socket.getPort());
		}

	}

	private void die() {
		ServerListener.getInstance().getExecutor().shutdownNow();
		System.out.println("Going to die...");
		System.exit(0);
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
			System.out.println(logPrefix + "Error: Socket Close: " + e.getMessage() + " from: "
					+ socket.getInetAddress().getHostName());
		}
	}

	/**
	 * save last state to memory and delete file
	 * 
	 * @param state
	 * @param packet
	 */
	private void createFailureFile(StateT state, CommitRequestPacket packet) {
		System.out.println(logPrefix + "Failing at state: " + state);

		File file = new File("failed_" + Definitions.thisHostName + "_" + process.getPortNo());

		try {
			if (file.createNewFile())
				System.out.println(logPrefix + file.getName() + " File is created!");
			try (PrintWriter out = new PrintWriter(file)) {
				out.println(packet.getTransactionId() + " " + packet.getCommitValue() + " " + state.getVal());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param logPrefix
	 * @param type
	 * @param packet
	 * @return isAllProcessAgreed
	 */
	public boolean sendCommitReqToAllNeighbors(String logPrefix, PacketType type, Packet packet) {
		currentState = StateT.WAIT;
		MeNode process = MeNode.getInstance();
		Map<Integer, Streams> sockets = process.getSocketStreamMap();

		for (Entry<Integer, Node> entry : process.getNeighbors().entrySet()) {
			Node neighbor = entry.getValue();
			Integer id = entry.getKey();
			try {

				Streams streams = sockets.get(neighbor.getProcessId());
				ObjectInputStream ois;
				ObjectOutputStream oos;

				if (streams == null) {
					try {
						Socket clientSocket = new Socket(neighbor.getHostName(), neighbor.getPortNo());
						clientSocket.setSoTimeout(Definitions.SOCKET_TIMEOUT);
						oos = new ObjectOutputStream(clientSocket.getOutputStream());
						ois = new ObjectInputStream(clientSocket.getInputStream());
						streams = new Streams(oos, ois);
						sockets.put(id, streams);
					} catch (IOException e) {
						System.out.println(e.getMessage());
						continue;
					}
				}
				oos = streams.getOos();
				ois = streams.getOis();

				switch (type) {

				case COMMIT_REQ:
					System.out.println(logPrefix + "Send " + type + " to " + neighbor.getProcessId());
					oos.writeObject(packet);
					oos.flush();
					break;

				default:
					break;
				}

				Object readObject = ois.readObject();
				if (readObject instanceof AgreedPacket) {
					AgreedPacket responsePacket = (AgreedPacket) readObject;
					System.out.println(logPrefix + "AGREED received from <= " + responsePacket.getSenderId());
					coordinator.addToAgreedNodes(responsePacket.getSenderId());
				} else if (readObject instanceof AbortPacket) {
					AbortPacket responsePacket = (AbortPacket) readObject;
					System.out.println(logPrefix + "ABORT received from <= " + responsePacket.getSenderId());
				}
			} catch (IOException e) {
				System.out.println(logPrefix + "Error: Send msg => " + entry.getKey() + ": " + neighbor.getHostName()
						+ ":" + neighbor.getPortNo() + " " + e.getMessage());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		if (coordinator.isAllProcessAgreed()) {
			// COORD received all AGREED
			return true;
		}
		return false;
	}

	public boolean sendPrepareReqToAllNeighbors(String logPrefix, PacketType type, Packet packet) {
		currentState = StateT.PREPARE;
		MeNode process = MeNode.getInstance();
		Map<Integer, Streams> sockets = process.getSocketStreamMap();

		for (Entry<Integer, Node> entry : process.getNeighbors().entrySet()) {
			Node neighbor = entry.getValue();
			Integer id = entry.getKey();
			try {

				Streams streams = sockets.get(neighbor.getProcessId());
				ObjectInputStream ois;
				ObjectOutputStream oos;

				if (streams == null) {
					try {
						Socket clientSocket = new Socket(neighbor.getHostName(), neighbor.getPortNo());
						oos = new ObjectOutputStream(clientSocket.getOutputStream());
						ois = new ObjectInputStream(clientSocket.getInputStream());
						clientSocket.setSoTimeout(Definitions.SOCKET_TIMEOUT);
						streams = new Streams(oos, ois);
						sockets.put(id, streams);
					} catch (IOException e) {
						System.out.println(e.getMessage());
						continue;
					}
				}
				oos = streams.getOos();
				ois = streams.getOis();

				switch (type) {

				case PREPARE:
					System.out.println(logPrefix + "Send " + type + " to " + neighbor.getProcessId());
					oos.writeObject(packet);
					oos.flush();
					break;

				default:
					break;
				}

				Object readObject = ois.readObject();
				if (readObject instanceof AckPacket) {
					AckPacket responsePacket = (AckPacket) readObject;
					System.out.println(logPrefix + "ACK received from <= " + responsePacket.getSenderId());
					coordinator.addToAckNodes(responsePacket.getSenderId());
					if (coordinator.isAllProcessAck()) {
						// COORD received all AGREED
						return true;
					}
				}
			} catch (IOException e) {
				System.out.println(logPrefix + "Error: Send " + type + " => " + entry.getKey() + ": "
						+ neighbor.getHostName() + ":" + neighbor.getPortNo() + " " + e.getMessage());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public void sendCommitToAllNeighbors(String logPrefix, PacketType type, Packet packet) {
		currentState = StateT.COMMIT;
		MeNode process = MeNode.getInstance();
		Map<Integer, Streams> sockets = process.getSocketStreamMap();

		for (Entry<Integer, Node> entry : process.getNeighbors().entrySet()) {
			Node neighbor = entry.getValue();
			Integer id = entry.getKey();
			try {

				Streams streams = sockets.get(neighbor.getProcessId());
				ObjectInputStream ois;
				ObjectOutputStream oos;

				if (streams == null) {
					try {
						Socket clientSocket = new Socket(neighbor.getHostName(), neighbor.getPortNo());
						oos = new ObjectOutputStream(clientSocket.getOutputStream());
						ois = new ObjectInputStream(clientSocket.getInputStream());
						clientSocket.setSoTimeout(Definitions.SOCKET_TIMEOUT);
						streams = new Streams(oos, ois);
						sockets.put(id, streams);
					} catch (IOException e) {
						System.out.println(e.getMessage());
						continue;
					}
				}
				oos = streams.getOos();
				ois = streams.getOis();

				switch (type) {

				case COMMIT:
					System.out.println(logPrefix + "Send " + type + " to " + neighbor.getProcessId());
					oos.writeObject(packet);
					oos.flush();
					break;

				default:
					break;
				}

			} catch (IOException e) {
				System.out.println(logPrefix + "Error: Send " + type + " => " + entry.getKey() + ": "
						+ neighbor.getHostName() + neighbor.getPortNo() + e.getMessage());
			}
		}
		return;
	}

	public void writeToTransactionFile(String logPrefix, StateT state, CommitRequestPacket commitPacket) {
		System.out.println(
				"Transaction " + commitPacket.getTransactionId() + " completed with state: " + state + "\n*\n*\n*");

		String fileName = "transaction_" + Definitions.thisHostName + "_" + MeNode.getInstance().getPortNo();
		int lineCount = commitPacket.getTransactionId();

		try (FileWriter fw = new FileWriter(fileName, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println("date: " + df.format(new Date()) + " T: " + lineCount + " value: "
					+ commitPacket.getCommitValue() + " status: " + state+" failedAt: "+((failAtState ==null) ? "NONE" : failAtState.getVal()));
		} catch (IOException e) {
			System.out.println(logPrefix + "Write transaction error: " + e.getMessage());
		}
		this.commitPacket = null;
	}

}
