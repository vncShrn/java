*Readme file for Project - Fog Node Implementation
*CS 6390 - Advanced Computer Networks 
*Fall 2016
*Author:Manisha Arumugam, Vincy Shrine, Apoorva Jayaram

1. Copy fogNode.jar and fogConfig<fogId>.properties to a folder in the fog node

2. In order to run this JAR file, you need to have Java 8 installed on your machine

3. Change directory to the folder containing the jar and config file
cd location/of/the/jar

4. Edit the fog configuration properties as follows:
vi fogConfig<fogId>.properties

		# Assign fog id according to the provided topology (as integer)
		MY_ID=5

		# Enter the hostname/IP address of this fog node (as String)
		MY_IP=127.0.0.1

		# Enter UDP port of this fog node; datatype (as integer)
		MY_UDP=7005

		# Enter TCP port of this fog node; datatype (as integer)
		MY_TCP=9005

		# Enter the maximum response time of this fog node (in ms)
		Max_Response_Time=40000

		# Enter the list of neighbors of this fog node, based on the given topology in below format:
		# Neigbours=N1:TCP1:ID1,N2:TCP2:ID2,N3:TCP3:ID3
		Neigbours=127.0.0.1:9003:3,127.0.0.1:9004:4

		# Provide mapping of request type to time in ms (request type comes along with packet info)
		#Request_Type=Type1:Time1_in_ms,Type2:Time2_in_ms
		Request_Type=1:1000,2:2000,3:3000,4:4000,5:5000,6:5000,7:5000,8:5000,9:5000,10:5000

5. Now you can run the FogNode with the following command:
java -jar fogNode.jar fogConfig<fogId>.properties > outFog<fogId>.txt 2>&1 &
