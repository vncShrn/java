package com.aos.me.main;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;

import com.aos.me.listener.ServerListener;
import com.aos.me.other.AlgorithmType;
import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.packets.Packet;
import com.aos.me.packets.PacketType;
import com.aos.me.packets.RegisterPacket;
import com.aos.me.poll.DidAllNeighborsHandshake;
import com.aos.me.process.MeNode;

/**
 * Main class to instantiate either one of the Mutual Exclusion algorithms, for
 * a DS: <br>
 * 1. Suzuki Kazami <br>
 * 2. Raymond's Tree
 * 
 * @author Vincy Shrine
 *
 */
public class MutualExclusionImpl {

	public static void main(String[] args) throws UnknownHostException {

		// Process cmd line args, config file
		processInputs(args);

		MeNode process = MeNode.getInstance();

		if (Definitions.AM_I_COORDINATOR) {
			Definitions.PID = 1;
			ServerListener server = ServerListener.getInstance();
			server.setPortNo(Definitions.COORD_PORT);
			Thread tcpReceiverThread = new Thread(server);
			tcpReceiverThread.start();
		} else {
			// 1. Send REGISTER msg to COORDINATOR
			Socket coordSocket = null;
			try {
				coordSocket = new Socket(Definitions.COORDINATOR_HOSTNAME, Definitions.COORD_PORT);
			} catch (IOException e) {
				System.err.println("REGISTRATION error: " + e.getMessage()+	 InetAddress.getLocalHost().getCanonicalHostName());
				System.exit(0);
			}
			Packet packet = (new Helper()).sendTcpMessage(new RegisterPacket(), coordSocket, PacketType.REGISTER + " ");
			if (packet instanceof RegisterPacket) {
				RegisterPacket registerPacket = (RegisterPacket) packet;
				process.setProcessId(registerPacket.getProcessId());
				Definitions.PID = registerPacket.getProcessId();
				process.setPortNo(registerPacket.getPortNo());
				System.out.println("REGISTRATION complete. Obtained processID: " + process.getId() + ", portNo: "
						+ process.getPortNo());
			}
			// 2. Start Listening to neighbors on port
			ServerListener server = ServerListener.getInstance();
			server.setPortNo(process.getPortNo());
			Thread tcpReceiverThread = new Thread(server);
			tcpReceiverThread.start();

		}

		// Process keeps checking if it received HELLO from all
		// if received all, then sends READY to COORD, and self-kill below
		// thread
		Timer timer = new Timer();
		timer.schedule(new DidAllNeighborsHandshake(), 0, 100);

	}

	/**
	 * Process cmd line args and config file
	 * 
	 * @param args
	 */
	private static void processInputs(String[] args) {
		boolean argsCoord = false;
		if (args.length > 0) {
			if (args[0].equals("1")) {
				Definitions.ALGORITHM = AlgorithmType.SUZUKI_KAZAMI;
			} else if (args[0].equals("2")) {
				Definitions.ALGORITHM = AlgorithmType.RAYMOND_TREE;
			} else {
				System.out
						.println("USAGE: java -jar dproc.jar <algorithm_id> -c" + "\n algorithm_id = 1 (Suzuki kazami)"
								+ "\n algorithm_id = 2 (Raymond's tree)" + "\n -c = denotes coordinator (optional)");
				System.exit(0);
			}
			if (args.length > 1 && ("-c").equals(args[1]))
				argsCoord = true;

		} else {
			System.out.println("USAGE: java -jar jarname.jar <algorithm_id> -c" + "\n algorithm_id = 1 (Suzuki kazami)"
					+ "\n algorithm_id = 2 (Raymond's tree)" + "\n -c = denotes coordinator (optional)");
			System.exit(0);
		}

		/** Read config file - Begin */
		File configFile = new File("dsConfig");
		if (!configFile.exists()) {
			System.err.println("Config file \"dsConfig\" does not exist");
			System.exit(0);
		}

		try {
			Helper.readConfig(configFile, argsCoord);
		} catch (UnknownHostException e) {
			System.err.println("Config file error: " + e.getMessage());
			System.exit(0);
		}
		/** Read config file - End */

	}
}
