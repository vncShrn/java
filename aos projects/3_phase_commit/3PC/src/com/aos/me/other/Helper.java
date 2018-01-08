package com.aos.me.other;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.aos.me.models.Coordinator;
import com.aos.me.models.MeNode;
import com.aos.me.models.Node;
import com.aos.me.models.Streams;
import com.aos.me.packets.AbortPacket;
import com.aos.me.packets.AckPacket;
import com.aos.me.packets.AgreedPacket;
import com.aos.me.packets.CommitRequestPacket;
import com.aos.me.types.Packet;
import com.aos.me.types.PacketType;
import com.aos.me.types.StateT;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class Helper {

	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	public static void readConfig(File configFile, int pid) throws UnknownHostException {
		boolean amICoord = false;
		if (pid == 1)
			amICoord = true;
		String thisHostName = InetAddress.getLocalHost().getCanonicalHostName();
		try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
			String line = br.readLine();
			String coordHostName = line.substring(line.lastIndexOf(" ") + 1);
			Definitions.COORDINATOR_HOSTNAME = coordHostName;
			line = br.readLine();
			Definitions.MAX_PROCESS = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
			line = br.readLine();

			int coordPortNo = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
			Definitions.COORD_PORT = coordPortNo;

			MeNode node = MeNode.getInstance();

			node.setHostName(thisHostName);
			Definitions.thisHostName = thisHostName;
			node.setProcessId(pid);
			Definitions.PID = pid;

			if (amICoord == true && thisHostName.equalsIgnoreCase(coordHostName)) {
				Definitions.AM_I_COORDINATOR = true;
				node.setProcessId(1);
				Definitions.PID = 1;
				node.setPortNo(coordPortNo);
				System.out.println("Process " + thisHostName + ": I AM THE COORDINATOR");
			}
			Map<Integer, Node> networkMap = new HashMap<>();
			for (int i = 2; (line = br.readLine()) != null; i++) {
				String lineValues[] = line.trim().split(" ");

				if (lineValues.length > 1) {
					String hostName = lineValues[0];
					int portNo = Integer.parseInt(lineValues[1]);
					networkMap.put(i, new Node(i, hostName, portNo));
					if (thisHostName.equals(hostName) && !Definitions.AM_I_COORDINATOR && pid == i) {
						node.setHostName(hostName);
						node.setPortNo(portNo);
						System.out.println("I am cohort. My process id: " + pid + " hostName: " + hostName);
						break;
					}
				}
			}
			if (Definitions.AM_I_COORDINATOR) {
				MeNode.getInstance().setNeighbors(networkMap);
			} else {
				Map<Integer, Node> neighborMap = new HashMap<Integer, Node>();
				neighborMap.put(1, new Node(1, Definitions.COORDINATOR_HOSTNAME, Definitions.COORD_PORT));
				MeNode.getInstance().setNeighbors(neighborMap);
			}

			System.out.println("<pid,neighbors> map: " + MeNode.getInstance().getNeighbors());
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return;
		}
	}

	/**
	 * 
	 * @param logPrefix
	 * @param type
	 * @param packet
	 * @return isAllProcessAgreed
	 */
	public boolean sendPacketToAllNeighbors(String logPrefix, PacketType type, Packet packet) {
		MeNode process = MeNode.getInstance();
		Map<Integer, Streams> sockets = process.getSocketStreamMap();
		Coordinator coordinator = Coordinator.getInstance();

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
				System.out.println(logPrefix + "Send " + type + " to " + neighbor.getProcessId());

				switch (type) {

				case COMMIT_REQ: {
					oos.writeObject(packet);
					oos.flush();
					Object readObject = ois.readObject();
					if (readObject instanceof AgreedPacket) {
						AgreedPacket responsePacket = (AgreedPacket) readObject;
						System.out.println(logPrefix + "AGREED received from <= " + responsePacket.getSenderId());
						coordinator.addToAgreedNodes(responsePacket.getSenderId());
					} else if (readObject instanceof AbortPacket) {
						AbortPacket responsePacket = (AbortPacket) readObject;
						System.out.println(logPrefix + "ABORT received from <= " + responsePacket.getSenderId());
					}
					if (coordinator.isAllProcessAgreed()) {
						// COORD received all AGREED
						return true;
					}
				}
					break;
				case PREPARE:
					oos.writeObject(packet);
					oos.flush();

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
					break;
				case COMMIT:
					oos.writeObject(packet);
					oos.flush();
					break;
				case ABORT:
					oos.writeObject(packet);
					oos.flush();
					break;
				case TERMINATE:
					oos.writeObject(packet);
					oos.flush();
					break;
				default:
					return false;
				}

			} catch (IOException e) {
				System.out.println(logPrefix + "Error: Send msg => " + entry.getKey() + ": " + neighbor.getHostName()
						+ ":" + neighbor.getPortNo() + " " + e.getMessage());
				sockets.remove(neighbor.getProcessId());

			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	public void sendPacketToNeighbor(String logPrefix, PacketType type, Packet token, int id) {

		MeNode process = MeNode.getInstance();
		Node neighbor = null;

		if (id == process.getId() && Definitions.AM_I_COORDINATOR)
			neighbor = new Node(1, process.getHostName(), Definitions.COORD_PORT);
		else
			neighbor = process.getNeighbors().get(id);

		Map<Integer, Streams> sockets = process.getSocketStreamMap();
		Streams streams = sockets.get(neighbor.getProcessId());
		ObjectInputStream ois;
		ObjectOutputStream oos;

		if (streams == null) {
			try {
				Socket clientSocket = new Socket(neighbor.getHostName(), neighbor.getPortNo());
				oos = new ObjectOutputStream(clientSocket.getOutputStream());
				ois = new ObjectInputStream(clientSocket.getInputStream());
				streams = new Streams(oos, ois);
				sockets.put(id, streams);

			} catch (IOException e) {
				System.out.println(logPrefix + "Error: Send msg => " + id + ": " + neighbor.getHostName()
						+ neighbor.getPortNo() + e.getMessage());
			}
		}
		oos = streams.getOos();
		ois = streams.getOis();
		try {
			System.out.println(logPrefix + "Send " + type + " to => " + id);
			oos.writeObject(token);
			oos.flush();
		} catch (IOException e) {
			System.out.println(logPrefix + "Error: Send msg => " + id + ": " + neighbor.getHostName()
					+ neighbor.getPortNo() + e.getMessage());
			sockets.remove(neighbor.getProcessId());
		}
		return;

	}

	public int countLines(String filename) {
		int cnt = 0;
		try {
			LineNumberReader reader = new LineNumberReader(new FileReader(filename));

			while ((reader.readLine()) != null) {
			}

			cnt = reader.getLineNumber();

			reader.close();
		} catch (IOException e) {
			System.out.println("Line count error: " + e.getMessage());
		}
		return cnt;
	}

	public void writeToTransactionFile(String logPrefix, StateT state, CommitRequestPacket commitPacket,
			boolean isRecovery) {
		System.out.println(
				"Transaction " + commitPacket.getTransactionId() + " completed with state: " + state + "\n*\n*\n*");

		String fileName = "transaction_" + Definitions.thisHostName + "_" + MeNode.getInstance().getPortNo();

		try (FileWriter fw = new FileWriter(fileName, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)) {
			out.println("T= " + commitPacket.getTransactionId() 
			+ " value= " + commitPacket.getCommitValue()
			+ " status= " + state 
			+ " date= " + df.format(new Date()) 
			+ (isRecovery ? " onRecovery" : ""));
		} catch (IOException e) {
			System.out.println(logPrefix + "Write transaction error: " + e.getMessage());
		}
		commitPacket = null;
	}

	/**
	 * save last state to memory and delete file
	 * 
	 * @param state
	 * @param packet
	 */
	public void createFailureFile(String logPrefix, StateT state, CommitRequestPacket packet) {
		MeNode process = MeNode.getInstance();

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

}
