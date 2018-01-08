package com.aos.me.threads;

import java.io.File;
import java.util.Scanner;

import com.aos.me.models.MeNode;
import com.aos.me.other.Definitions;
import com.aos.me.other.Helper;
import com.aos.me.packets.CommitRequestPacket;
import com.aos.me.packets.TerminatePacket;
import com.aos.me.types.PacketType;
import com.aos.me.types.StateT;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class CommitRequestThread extends Thread {

	private String logPrefix;
	private Helper helper = new Helper();

	public CommitRequestThread(String log) {
		this.logPrefix = log + " ";
	}

	public void run() {
		
		/** ME - CS= Request starts */
		MeNode process = MeNode.getInstance();

		Scanner scanner = new Scanner(System.in);
		int T = 1;

		String fileName = "transaction_" + Definitions.thisHostName + "_" + MeNode.getInstance().getPortNo();
		if (new File(fileName).exists()) 
			T = helper.countLines(fileName) + 1;

		while (true) {

			System.out.println(logPrefix + "Enter next transaction details in format" + "\n1. No failures: 1 <val> "
					+ "\n2. cohort disagrees: 2 <cohort_id> <val>" + "\n3. coord fails: 3 <q,w,p,c> <val>"
					+ "\n4. cohort fails: 4 <cohort_id> <q,w,p,c> <val>" + "\n5. quit");
			String input = scanner.nextLine();

			CommitRequestPacket packet = null;

			try {
				String[] split = input.split(" ");
				switch (Integer.parseInt(split[0])) {
				case 1:
					System.out.println(logPrefix + "T= " + T + " No failures");
					packet = new CommitRequestPacket(T++, Integer.parseInt(split[1]));
					break;
				case 2:
					System.out.println(logPrefix + "T= " + T + " Cohort disagrees");
					packet = new CommitRequestPacket(T++, Integer.parseInt(split[1]), Integer.parseInt(split[2]));
					break;
				case 3:
					System.out.println(logPrefix + "T= " + T + " Coord fails");
					packet = new CommitRequestPacket(T++, StateT.getState(split[1]), Integer.parseInt(split[2]));
					break;
				case 4:
					System.out.println(logPrefix + "T= " + T + " Cohort fails");
					packet = new CommitRequestPacket(T++, Integer.parseInt(split[1]), StateT.getState(split[2]),
							Integer.parseInt(split[3]));
					break;
				case 5:
					scanner.close();
					System.out.println(logPrefix + "Completed all COMMIT requests. Going to TERMINATE...");
					helper.sendPacketToAllNeighbors(logPrefix, PacketType.TERMINATE, new TerminatePacket());
					process.getConsumerQThread().shutdown();
					System.exit(0);
					break;
				default:
					break;

				}
			} catch (Exception e) {
				System.out.println(logPrefix + "Invalid input \n");
				continue;
			}
			helper.sendPacketToNeighbor(logPrefix, PacketType.COMMIT_REQ, packet, 1);

			try {
				process.getTokenQ().take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
