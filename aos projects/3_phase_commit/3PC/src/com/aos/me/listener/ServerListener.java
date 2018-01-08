package com.aos.me.listener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.aos.me.other.Definitions;
import com.aos.me.process.MeNode;

/**
 * 
 * @author Vincy Shrine
 *
 */
public class ServerListener implements Runnable {

	private ExecutorService executor = Executors.newFixedThreadPool(Definitions.MAX_PROCESS + 5);
	private int portNo;
	SimpleDateFormat df = new SimpleDateFormat("hh:mm:ss.SSS", Locale.US);

	private static ServerListener server = null;

	public static synchronized ServerListener getInstance() {
		if (server == null) {
			server = new ServerListener();
		}
		return server;
	}

	private ServerListener() {
	}

	public void setPortNo(int portNo) {
		this.portNo = portNo;
	}

	public ExecutorService getExecutor() {
		return executor;
	}
	@Override
	public void run() {

		MeNode process = MeNode.getInstance();
		// String logPrefix = " Process " + process.getId() + ": -TCP LISTENER-
		// ";
		String logPrefix = " Process " + process.getId() + ": ";
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(portNo);
			serverSocket.setReuseAddress(true);
		} catch (IOException e) {
			System.out.println( logPrefix + "Error: Create socket: " + e.getMessage() + portNo);
		}
		System.out.println( logPrefix + "Waiting for incoming connections at: " + portNo);
		try {
			while (true) {
				// Listens for a connection and accepts it
				// The method blocks until a connection is made
				Socket incomingSocket = serverSocket.accept();
				// String hostName =
				// incomingSocket.getInetAddress().getCanonicalHostName();
				// process.addToSocketWriteMap(hostName, incomingSocket);
				// System.out.println(df.format(new Date())+logPrefix +
				// "Received packet from: " +
				// hostName);
				executor.execute(new ClientHandler(incomingSocket));
			}
		} catch (IOException e) {
			System.out.println( logPrefix + "Error: " + e.getMessage() + portNo);
			System.exit(1);
		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
}