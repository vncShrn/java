package com.aos.me.other;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.aos.me.packets.Node;
import com.aos.me.packets.Packet;
import com.aos.me.packets.PacketType;
import com.aos.me.process.Coordinator;
import com.aos.me.process.MeNode;

/**
 * @author Vincy Shrine
 */
public class Helper {

	static Random rand = new Random();
	static SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	public static void readConfig(File configFile, boolean argsCoord) throws UnknownHostException {
		String thisHostName = InetAddress.getLocalHost().getCanonicalHostName();
		try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
			String line = br.readLine();
			String coordHostName = line.substring(line.lastIndexOf(" ") + 1);
			Definitions.COORDINATOR_HOSTNAME = coordHostName;
			line = br.readLine();
			Definitions.MAX_PROCESS = Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
			line = br.readLine();
			Definitions.T1 = Long.parseLong(line.substring(line.lastIndexOf(" ") + 1).trim());
			line = br.readLine();
			Definitions.T2 = Long.parseLong(line.substring(line.lastIndexOf(" ") + 1).trim());
			line = br.readLine();
			Definitions.T3 = Long.parseLong(line.substring(line.lastIndexOf(" ") + 1).trim());

			// Double check that cmdline arg is -c and hostname is coordin's
			if (/* thisHostName.equals(coordHostName) && */ argsCoord)
				Definitions.AM_I_COORDINATOR = true;

			MeNode node = MeNode.getInstance();
			node.setHostName(thisHostName);

			if (Definitions.AM_I_COORDINATOR == true) {

				node.setProcessId(1);
				System.out.println(df.format(new Date()) + " Process " + thisHostName + ": I AM THE COORDINATOR");

				if (("PARENT LIST").equals(br.readLine())) {
					Map<Integer, Integer> networkMap = new HashMap<>();
					for (; (line = br.readLine()) != null;) {
						String lineValues[] = line.trim().split(" ");

						if (lineValues.length > 2) {
							// A child has more than one parents
						} else {
							Integer key = Integer.parseInt(lineValues[0]);
							int parentId = Integer.parseInt(lineValues[1]);
							networkMap.put(key, parentId);
						}
					}
					Coordinator.getInstance().setConfigMap(networkMap);
					Coordinator.getInstance().setHostName(thisHostName);
					System.out.println("Parent map: " + networkMap);
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return;
		}
	}

	public static Integer getNextRandom(int min, int max) {
		return rand.nextInt(max) + min;
	}

	public Packet sendTcpMessage(Object msg, Socket clientSocket, String logPrefix) {
		try {
			clientSocket.setReuseAddress(true);
			// String hostName =
			// clientSocket.getInetAddress().getCanonicalHostName();
			System.out.println(df.format(new Date())
					+ logPrefix /* + " msg sent to: " + hostName */);
			// Create a client socket and connect to server
			// clientSocket.setKeepAlive(true);
			OutputStream os = clientSocket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			InputStream is = clientSocket.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			oos.writeObject(msg);
			clientSocket.shutdownOutput();
			oos.flush();
			try {
				Object readObject = ois.readObject();
				if (readObject instanceof Packet) {
					// System.out.println("OOS Stream closed: " +
					// clientSocket.getInetAddress().getHostName());
					oos.close();
					ois.close();
					clientSocket.close();
					return (Packet) readObject;
				}
			} catch (IOException e) {
				// Ignore
				// System.out.println(df.format(new Date())+logPrefix + "Error:
				// No reply for sent msg:
				// " + e.getMessage());
			}
			// System.out.println("OOS Stream closed: " +
			// clientSocket.getInetAddress().getHostName());
			oos.close();
			ois.close();
			clientSocket.close();
		} catch (ClassNotFoundException e) {
			System.out.println(df.format(new Date()) + logPrefix + "Error: " + e.getMessage());
		} catch (IOException e) {
			System.out.println(df.format(new Date()) + logPrefix + "Error: " + e.getMessage());
		}
		return null;
	}

	public List<Integer> pickRandomNeighbors(Set<Integer> neighbors, int numberOfOutgoingMsgs) {
		List<Integer> sendToNeighborsList = new ArrayList<>(numberOfOutgoingMsgs);
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.addAll(neighbors);

		for (int i = 0; i < numberOfOutgoingMsgs; i++) {
			int x = rand.nextInt(list.size());
			int r = list.get(x);
			sendToNeighborsList.add(r);
		}
		return sendToNeighborsList;
	}

	public void writeToFile(String logPrefix, Map<Integer, String> localStateMap, String hostName) {

		BufferedWriter writer = null;
		try {
			File logFile = new File("localstate_" + hostName);

			writer = new BufferedWriter(new FileWriter(logFile));
			for (Entry<Integer, String> entry : localStateMap.entrySet()) {
				writer.write(entry.getValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (Exception e) {
			}
		}
	}

	public void sendPacketToCoord(String logPrefix, PacketType type, Packet packet) {
		Socket coordSocket = null;
		try {
			coordSocket = new Socket(Definitions.COORDINATOR_HOSTNAME, Definitions.COORD_PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (packet != null)
			(new Helper()).sendTcpMessage(packet, coordSocket, logPrefix + "Send " + type + " to CO-ORD");

	}

	public void sendPacketToAllProcess(String logPrefix, PacketType type, Packet packet) {
		MeNode process = MeNode.getInstance();
		Map<Integer, ObjectOutputStream> sockets = process.getSocketStreamMap();

		for (Entry<Integer, Node> entry : process.getProcessInformation().entrySet()) {
			Node child = entry.getValue();
			Integer id = entry.getKey();

			if (id == process.getId())
				continue;
			try {
				if (type.equals(PacketType.HELLO)) {
					Socket socket = new Socket(child.getHostName(), child.getPortNo());
					(new Helper()).sendTcpMessage(packet, socket,
							logPrefix + "Send HELLO " + " to => " + entry.getKey());
					continue;
				}
				ObjectOutputStream oos = sockets.get(child.getProcessId());

				if (oos == null) {
					// System.out.println("New OO stream created: "+id);
					try {
						Socket clientSocket = new Socket(child.getHostName(), child.getPortNo());
						oos = new ObjectOutputStream(clientSocket.getOutputStream());
						sockets.put(id, oos);
					} catch (IOException e) {
						System.out.println(df.format(new Date()) + logPrefix + e.getMessage());
						continue;
					}
				}

				switch (type) {
				case ME_START:
					System.out.println(df.format(new Date()) + logPrefix + "Send " + type + " to => " + id);
					oos.writeObject(packet);
					oos.flush();
					break;
				case TERMINATE:
					System.out.println(df.format(new Date()) + logPrefix + "Send " + type + " to => " + id);
					oos.writeObject(packet);
					oos.flush();
					break;
				case CS_REQ:
					System.out.println(df.format(new Date()) + logPrefix + "Send " + type + " to => " + id);
					oos.writeObject(packet);
					oos.flush();
					break;
				default:
					break;
				}
			} catch (IOException e) {
				System.out.println(df.format(new Date()) + logPrefix + "Error: Send msg => " + entry.getKey() + ": "
						+ child.getHostName() + child.getPortNo() + e.getMessage());
			}
		}
		return;
	}

	public void sendPacketToNeighbor(String logPrefix, PacketType type, Packet token, int id) {

		MeNode process = MeNode.getInstance();

		Node neighbor = null;

		if (id == 1 && Definitions.AM_I_COORDINATOR)
			neighbor = new Node(1, process.getHostName(), Definitions.COORD_PORT);
		else
			neighbor = process.getProcessInformation().get(id);

		Map<Integer, ObjectOutputStream> sockets = process.getSocketStreamMap();
		ObjectOutputStream oos = sockets.get(neighbor.getProcessId());

		if (oos == null) {
			try {
				Socket clientSocket = new Socket(neighbor.getHostName(), neighbor.getPortNo());
				oos = new ObjectOutputStream(clientSocket.getOutputStream());
				sockets.put(id, oos);

			} catch (IOException e) {
				System.out.println(df.format(new Date()) + logPrefix + "Error: Send msg => " + id + ": "
						+ neighbor.getHostName() + neighbor.getPortNo() + e.getMessage());
			}
		}
		try {
			System.out.println(df.format(new Date()) + logPrefix + "Send " + type + " to => " + id);
			oos.writeObject(token);
			oos.flush();
		} catch (IOException e) {
			System.out.println(df.format(new Date()) + logPrefix + "Error: Send msg => " + id + ": "
					+ neighbor.getHostName() + neighbor.getPortNo() + e.getMessage());
		}
		return;

	}

	// The previous code is throwing EOF exception
	public void sendResponse(String msg, Socket clientSocket, String logPrefix) {
		try {
			PrintStream printStream = new PrintStream(clientSocket.getOutputStream());
			printStream.println(msg);
			printStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
