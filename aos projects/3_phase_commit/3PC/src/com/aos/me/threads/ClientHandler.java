package com.aos.me.threads;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

import com.aos.me.models.Coordinator;
import com.aos.me.models.MeNode;
import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.packets.AbortPacket;
import com.aos.me.packets.AckPacket;
import com.aos.me.packets.AgreedPacket;
import com.aos.me.packets.CommitPacket;
import com.aos.me.packets.CommitRequestPacket;
import com.aos.me.packets.HelloPacket;
import com.aos.me.packets.PreparePacket;
import com.aos.me.packets.TerminatePacket;
import com.aos.me.types.PacketType;
import com.aos.me.types.StateT;

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
						currentState = StateT.REQ_RECEIVED;

						if (Definitions.AM_I_COORDINATOR) {
							if (commitPacket.isCoord()) {
								failAtState = commitPacket.getFailAtState();
							} else
								failAtState = null;

							System.out.println(logPrefix + "COORD received COMMIT_REQ: " + commitPacket);
							if (currentState != failAtState) {

								boolean isAllAgreedReceived = helper.sendPacketToAllNeighbors(logPrefix,
										PacketType.COMMIT_REQ, commitPacket);
								coordinator.resetAgreedSet();

								currentState = StateT.PREPARE;
								if (currentState == failAtState) {
									helper.createFailureFile(logPrefix, currentState, commitPacket);
									System.out.println(logPrefix
											+ "COORD fails before sending PREPARE, cohorts should timeout waiting for PREPARE and ABORT");
									System.out.println(logPrefix + "COORD on recovery ABORTS transaction");
									die();
								}

								// Move to phase 2
								// COORD sends PREPARE to all
								if (isAllAgreedReceived) {

									boolean isAllPrepareAckReceived = helper.sendPacketToAllNeighbors(logPrefix,
											PacketType.PREPARE, new PreparePacket());
									coordinator.resetAckSet();

									if (isAllPrepareAckReceived) {
										// Move to phase 3
										System.out.println(logPrefix
												+ "ACK received for all PREPARE, Transaction has to be commited");
										currentState = StateT.COMMIT;

										if (failAtState != currentState) {
											helper.sendPacketToAllNeighbors(logPrefix, PacketType.COMMIT,
													new CommitPacket());
											helper.writeToTransactionFile(logPrefix, currentState, commitPacket, false);
										} else {
											System.out.println(logPrefix
													+ "COORD fails before sending COMMIT, cohorts should timeout, go ahead and COMMIT");
											helper.createFailureFile(logPrefix, currentState, commitPacket);
											die();
										}
									} else {
										currentState = StateT.ABORT;
										// Someone has not responded with ACK
										// So send ABORT to all cohorts
										System.out.println(logPrefix
												+ "Transaction aborted, as someone timed out to reply ACK to PREPARE");
										helper.sendPacketToAllNeighbors(logPrefix, PacketType.ABORT, new AbortPacket());
										helper.writeToTransactionFile(logPrefix, currentState, commitPacket, false);
									}

								} else {
									currentState = StateT.ABORT;
									// Someone has not responded with Agreed
									// message
									// So send ABORT to all cohorts
									System.out.println(
											logPrefix + "Transaction aborted, as someone did not agree to COMMIT_REQ");
									helper.sendPacketToAllNeighbors(logPrefix, PacketType.ABORT, new AbortPacket());
									helper.writeToTransactionFile(logPrefix, currentState, commitPacket, false);
								}
							} else {
								if (failAtState == currentState) {
									helper.createFailureFile(logPrefix, currentState, commitPacket);
									System.out.println(logPrefix + "COORD fails at state: " + failAtState);
									die();
								} else {
									currentState = StateT.ABORT;
									System.out.println(logPrefix + "COORD timesout at state: " + isReplyAbort);
									helper.writeToTransactionFile(logPrefix, currentState, commitPacket, false);
								}
							}
							logPrefix = " Process " + process.getId() + ": ";
							process.getTokenQ().put(new String());
						} else {
							// COHORT handling of COMMIT_REQ
							System.out.println(
									"*\n*\n*\n" + logPrefix + "COHORT received COMMIT_REQ from <= 1" + commitPacket);

							if (!commitPacket.isCoord() && commitPacket.getCohortId() == process.getId()) {
								failAtState = commitPacket.getFailAtState();
								isReplyAbort = commitPacket.isReplyAbort();
							} else
								failAtState = null;
							
							if (failAtState == currentState) {
								helper.createFailureFile(logPrefix, currentState, commitPacket);
								die();
							} else {
								currentState = StateT.WAIT;
								if (isReplyAbort) {
									oos.writeObject(new AbortPacket());
									oos.flush();
									System.out.println(logPrefix + "Send ABORT to => 1");
								} else {
									oos.writeObject(new AgreedPacket());
									oos.flush();
									System.out.println(logPrefix + "Send AGREED to => 1");
								}
							}
						}
					} else if (readObject instanceof PreparePacket) {
						System.out.println(logPrefix + "Received PREPARE from <= 1");
						currentState = StateT.PREPARE;
						// cohort fails while waiting for COMMIT from coord
						if (failAtState == currentState) {
							helper.createFailureFile(logPrefix, currentState, commitPacket);
							die();
						} else {
							oos.writeObject(new AckPacket());
							oos.flush();
							System.out.println(logPrefix + "Send ACK to => 1");
						}

					} else if (readObject instanceof CommitPacket) {
						currentState = StateT.COMMIT;
						System.out.println(logPrefix + "Received COMMIT from <= 1");
						if (failAtState != currentState)
							helper.writeToTransactionFile(logPrefix, currentState, commitPacket, false);
						else {
							helper.createFailureFile(logPrefix, currentState, commitPacket);
							die();
						}
					} else if (readObject instanceof AbortPacket) {
						currentState = StateT.ABORT;
						System.out.println(logPrefix + "Received ABORT from <= 1");
						if (failAtState != currentState)
							helper.writeToTransactionFile(logPrefix, currentState, commitPacket, false);
						else {
							helper.createFailureFile(logPrefix, currentState, commitPacket);
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
						helper.writeToTransactionFile(logPrefix, currentState, commitPacket, false);
					} else {
						System.out.println(logPrefix + "Cohort timed out at state: " + currentState);
						currentState = StateT.ABORT;
						helper.writeToTransactionFile(logPrefix, currentState, commitPacket, false);
					}
				} else {
					System.out
							.println(logPrefix + "COORD timed out at state: " + currentState + " : " + ex.getMessage());
				}
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

}
