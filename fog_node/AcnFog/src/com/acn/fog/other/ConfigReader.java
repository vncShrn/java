package com.acn.fog.other;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.acn.fog.resptime.FogNode;
import com.acn.fog.resptime.NeighborNode;

public class ConfigReader {

	private static Map<Integer, Long> reqMap = new HashMap<>();

	public static void readProperties(String configFile) {
		Properties prop = new Properties();
		try {
			prop.load(new BufferedInputStream(new FileInputStream(configFile)));
			String requestTypeString = String.valueOf(prop.get(FogDefinitions.REQUEST_TYPE));
			reqMap = formRequestTypeMap(requestTypeString);

			FogNode fogNode = FogNode.getInstance();

			fogNode.setId(Integer.parseInt(String.valueOf(prop.get(FogDefinitions.MY_ID))));
			fogNode.setIpAddress(String.valueOf(prop.get(FogDefinitions.MY_IP)));
			fogNode.setTcpPortNo(Integer.parseInt(String.valueOf(prop.get(FogDefinitions.MY_TCP))));
			fogNode.setUdpPortNo(Integer.parseInt(String.valueOf(prop.get(FogDefinitions.MY_UDP))));
			fogNode.setMaxResponseTime(Long.parseLong(String.valueOf(prop.get(FogDefinitions.MAXRT))));
			fogNode.setNeighbors(formNeighborsList(String.valueOf(prop.get(FogDefinitions.NEIGHBORS))));

			System.out.println(new Date() + "Fog config file parsed: " + fogNode.toString());

		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(0);
		}
	}

	private static List<NeighborNode> formNeighborsList(String neighborsString) {
		List<NeighborNode> neighbors = new ArrayList<>();

		try {
			String[] neighborsArr = neighborsString.split(",");
			for (String element : neighborsArr) {
				String[] value = element.split(":");
				neighbors.add(new NeighborNode(value[0], Integer.parseInt(value[1]), Integer.parseInt(value[2])));
			}
		} catch (NullPointerException ex) {
			System.err.println("Null value found for requestType.. Hence exiting");
			System.exit(0);
		}
		return neighbors;
	}

	private static Map<Integer, Long> formRequestTypeMap(String requestTypeString) {
		Map<Integer, Long> reqMap = new HashMap<>();

		try {
			String[] reqTypeArr = requestTypeString.split(",");
			for (String element : reqTypeArr) {
				String[] value = element.split(":");
				reqMap.put(Integer.parseInt(value[0]), Long.parseLong(value[1]));
			}
		} catch (NullPointerException ex) {
			System.err.println("Null value found for requestType.. Hence exiting");
			System.exit(0);
		}
		return reqMap;
	}

	public static Map<Integer, Long> getReqMap() {
		return reqMap;
	}

}
