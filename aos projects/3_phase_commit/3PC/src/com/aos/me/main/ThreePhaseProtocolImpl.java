package com.aos.me.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.Scanner;

import com.aos.me.models.MeNode;
import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.packets.CommitRequestPacket;
import com.aos.me.packets.HelloPacket;
import com.aos.me.threads.CommitRequestThread;
import com.aos.me.threads.ServerListener;
import com.aos.me.types.PacketType;
import com.aos.me.types.StateT;

/**
 * Main class to instantiate Three Phase Commit Protocol, for a DS
 * 
 * @author Vincy Shrine
 *
 */
public class ThreePhaseProtocolImpl {

	public static void main(String[] args) throws UnknownHostException, InterruptedException {

		// Process cmd line args, config file
		processInputs(args);

		MeNode process = MeNode.getInstance();
		
		boolean isJustRecovered= false;

		File failedFile = new File("failed_" + Definitions.thisHostName+"_"+process.getPortNo());
		if (failedFile.exists()) {
			// the process has just recovered

			Scanner sc;
			try {
				sc = new Scanner(failedFile);
				if (sc.hasNext()) {
					CommitRequestPacket packet = new CommitRequestPacket(sc.nextInt(), sc.nextInt());
					StateT previouslyFailedAtState = StateT.getState(sc.next());
					System.out.println(Definitions.thisHostName
							+ " Previously failed T= " + packet.getTransactionId()  + " at state= " + previouslyFailedAtState);

					// commit the transaction on recovery
					switch (previouslyFailedAtState) {
					case COMMIT:
						(new Helper()).writeToTransactionFile("", StateT.COMMIT, packet, true);
						break;
					default:
						(new Helper()).writeToTransactionFile("", StateT.ABORT, packet, true);
						break;
					}
				}
				sc.close();
				if (failedFile.delete()) {
					System.out.println("File deleted " + failedFile.getName());
				} else {
					System.out.println("Failed to delete file " + failedFile.getName());
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			isJustRecovered = true;
		} else {
			// Create new transaction file
			/*String fileName = "transaction_" + Definitions.thisHostName + "_" + process.getPortNo();
			try {
				FileWriter fw = new FileWriter(fileName, false);
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
		}

		if (Definitions.AM_I_COORDINATOR) {
			Definitions.PID = 1;
			ServerListener server = ServerListener.getInstance();
			server.setPortNo(Definitions.COORD_PORT);
			Thread tcpReceiverThread = new Thread(server);
			tcpReceiverThread.start();
			if (isJustRecovered){
				Thread.sleep(2000);
				Thread commitReqThread = new Thread(new CommitRequestThread("Process 1:"));
				commitReqThread.start();
			}
		} else {

			// 1. Send HELLO msg to COORDINATOR
			(new Helper()).sendPacketToNeighbor("Process " + process.getId() + " ", PacketType.HELLO, new HelloPacket(),
					1);

			// 2. Start Listening to neighbors on port
			ServerListener server = ServerListener.getInstance();
			server.setPortNo(process.getPortNo());
			Thread tcpReceiverThread = new Thread(server);
			tcpReceiverThread.start();

		}

	}

	/**
	 * Process cmd line args and config file
	 * 
	 * @param args
	 */
	private static void processInputs(String[] args) {
		if (args.length > 0) {
			int pid = Integer.parseInt(args[0]);

			/** Read config file - Begin */
			File configFile = new File("dsConfig");
			if (!configFile.exists()) {
				System.err.println("Config file \"dsConfig\" does not exist");
				System.exit(0);
			}

			try {
				Helper.readConfig(configFile, pid);
			} catch (UnknownHostException e) {
				System.err.println("Config file error: " + e.getMessage());
				System.exit(0);
			}
			/** Read config file - End */

		} else {
			System.out.println("USAGE: java -jar 3pc.jar <pid>" + "\n <pid> = 1 denotes coordinator ");
			System.exit(0);
		}

	}
	
}
