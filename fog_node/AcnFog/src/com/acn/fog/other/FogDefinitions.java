package com.acn.fog.other;

public class FogDefinitions {
	
	public static final String CONFIG_FILE = "fogConfig1.properties";
	public static final String REQUEST_TYPE = "Request_Type";
	public static final String NEIGHBORS = "Neigbours";
	public static final String MAXRT = "Max_Response_Time";
	public static final String MY_ID = "MY_ID";
	public static final String MY_IP = "MY_IP";
	public static final String MY_UDP = "MY_UDP";
	public static final String MY_TCP = "MY_TCP";
	public static final int FOG_QUEUE_SIZE = 100;
	public static final int CLOUD_QUEUE_SIZE = 200;
	// can recieve UDP responses up to 20Kb
	public static final int UDP_BUFFER_SIZE = 20000;
	public static final int TCP_BUFFER_SIZE = 50000;
	public static final int PRIORITY_Q_SIZE = 10;
	public static final long TIMER_TASK_INITIAL_DELAY = 2000;
	public static final long RESP_TIME_SEND_TASK_INTERVAL = 100;

}
