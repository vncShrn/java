package com.acn.fog.main;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

import com.acn.fog.consumer.CloudConsumer;
import com.acn.fog.consumer.FogConsumer;
import com.acn.fog.listener.Packet;
import com.acn.fog.listener.TcpListener;
import com.acn.fog.listener.UdpListener;
import com.acn.fog.other.ConfigReader;
import com.acn.fog.other.FogDefinitions;
import com.acn.fog.resptime.ResponseTimeUpdateTask;

public class FogNodeImpl {
	
	private ArrayBlockingQueue<Packet> fogQ;
	
	private ArrayBlockingQueue<Packet> cloudQ;
	
	public void initialize(String inputConfig) {
		
		ConfigReader.readProperties(inputConfig);
		
		fogQ = new ArrayBlockingQueue<>(FogDefinitions.FOG_QUEUE_SIZE);
		cloudQ = new ArrayBlockingQueue<>(FogDefinitions.CLOUD_QUEUE_SIZE);
		
    	TimerTask responseTimeSendTask = new ResponseTimeUpdateTask();
    	Timer timer = new Timer();
    	timer.schedule(responseTimeSendTask, FogDefinitions.TIMER_TASK_INITIAL_DELAY, FogDefinitions.RESP_TIME_SEND_TASK_INTERVAL);
		
		Thread tcpReceiverThread = new Thread(new TcpListener(fogQ, cloudQ));
		
		Thread udpReceiverThread = new Thread(new UdpListener(fogQ, cloudQ));
		
		Thread fogConsumerThread = new Thread(new FogConsumer(fogQ));
		
		Thread cloudConsumerThread = new Thread(new CloudConsumer(cloudQ));
		
		tcpReceiverThread.start();
		fogConsumerThread.start();
		cloudConsumerThread.start();
		udpReceiverThread.start();
		try {
			tcpReceiverThread.join();
			fogConsumerThread.join();
			cloudConsumerThread.join();
			udpReceiverThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}

	public static void main(String[] args) {
		
//		FogNode Max_Response_Time t MY_IP MY_UDP MY_TCP N1 TCP1 N2 TCP2
		
		FogNodeImpl fogNode = new FogNodeImpl();
		fogNode.initialize(args[0]);
		// TODO Manisha : error handling cmd line
	}

}
