package base;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.WriteAbortedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import base.MasterServerNode.ServerData;
import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Config;
import Utility.HeartBeat;
import Utility.HeartBeat.serverStatus;
import Utility.Message;
import Utility.NamespaceNode;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.SOSMessage;
import Utility.SOSMessage.msgTypeToMaster;
import Utility.SOSMessage.msgTypeToServer;

public class ChunkServerNode extends ServerNode {
	//public ClientServerNode client;
	//public MasterServerNode master;

	public class TFSFile {
		int fileNumber = 1;
		int spaceOccupied = 0;
		byte[] data = new byte[67108864];

		public TFSFile(int num){
			fileNumber = num;

		}
	}

	List<TFSFile> file_list = new ArrayList<TFSFile>();
	List<Message> messageList = Collections.synchronizedList(new ArrayList<Message>());

	public ChunkServerNode(String ip, int inPort, int outPort) {
		super(ip, inPort, outPort);
		
		myType = serverType.CHUNKSERVER;
		masterIP = Config.prop.getProperty("MASTERIP");
		masterPort = Integer.parseInt(Config.prop.getProperty("MASTER_INPORT"));
		for (int i = 0; i <= 4; i++){
			file_list.add(new TFSFile(i));
		}

		LoadServerNodeMap();
		LoadFileData();
	}

	String masterIP = null;
	int masterPort = 0;

	// hash to data

	Map<String, ChunkMetadata> chunkMap = new HashMap<String, ChunkMetadata>();

	/*
	 * public static void main(String argv[]) throws Exception
	 * 
	 * {
	 * 
	 * ServerSocket welcomeSocket = new ServerSocket(6789);
	 * 
	 * while(true) { Socket connectionSocket = welcomeSocket.accept();
	 * BufferedReader inFromClient = new BufferedReader(new
	 * InputStreamReader(connectionSocket.getInputStream())); DataOutputStream
	 * outToClient = new DataOutputStream(connectionSocket.getOutputStream());
	 * clientSentence = inFromClient.readLine(); System.out.println("Received: "
	 * + clientSentence); capitalizedSentence = clientSentence.toUpperCase() +
	 * '\n'; outToClient.writeBytes(capitalizedSentence); }
	 * 
	 *  //TODO:Timer that send out pings at regular intervals
	 * }
	 */

	/**
	 * @throws Exception
	 */
	public void main() throws Exception {	
		toString();
		try (ServerSocket mySocket = new ServerSocket(myInputPortNumber);)

		{
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
						HeartBeat HBMessage = new HeartBeat(myIP, myType, myInputPortNumber, 
								masterIP,serverType.MASTER, masterPort,serverStatus.ALIVE);
						SendMessage(HBMessage);
				}
			}, 10000, 10000);
			
			while(true) { 
				Socket otherSocket = mySocket.accept();
				ServerThread st = new ChunkServerThread(this, otherSocket);
				st.start();
				/*ObjectInputStream in = new ObjectInputStream(otherSocket.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(otherSocket.getOutputStream());
				Message incoming = (Message)in.readObject();
				if(incoming != null) {
					messageList.add(incoming);
					DealWithMessage();
					//outToClient.writeBytes(capitalizedSentence); 
				}*/
			}

			//TODO: Put in timer to increase TTL and check on status of all servers in ServerMap
			//TODO: Deal with Server Pings
			//TODO: Send updated chunkserver data to re-connected servers
		}
		catch (IOException e) {
			System.out
			.println("Exception caught when trying to listen on port "
					+ myInputPortNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
		finally{

		}
	}

	/**
	 * @param message
	 */
	public void ReadChunks(Message message){
		//		List<List<Byte>> fileMetaData = new ArrayList<List<Byte>>();
		//		for(ChunkLocation messageLocation: metadata.listOfLocations){
		//			for(File fileData: file_list){
		//				if((fileData.location.chunkIP == messageLocation.chunkIP) && (fileData.location.chunkPort == messageLocation.chunkPort)){
		//					fileMetaData.add(fileData.data);
		//				}
		//			}
		//		}
		//		
		for(TFSFile fileData:file_list){
			System.out.println("ChunkServer: Looking at file "+fileData.fileNumber + " looking for file " + message.chunkClass.filenumber);
			if(message.chunkClass.filenumber == fileData.fileNumber){
				System.out.println("ChunkServer: Available free byte size: "+(fileData.data.length-fileData.spaceOccupied));
				System.out.println("ChunkServer: Reading from file number "+message.chunkClass.filenumber);
				System.out.println("ChunkServer: Reading array size is "+message.chunkClass.size +" with byteoffset: "+message.chunkClass.byteoffset);
				System.out.println("ChunkServer: File data occupied space: "+fileData.spaceOccupied);


				byte[] dataINeed = new byte[message.chunkClass.size+4];
				// check byte offset
				int offSetIndex = message.chunkClass.byteoffset;
				for (int i = 0; i < message.chunkClass.size; i++) {
					dataINeed[i] = fileData.data[offSetIndex];
					offSetIndex++;
				}
				Message m = new Message(msgType.PRINTFILEDATA, myIP, myType, myInputPortNumber, message.senderIP, serverType.CLIENT, message.senderInputPort);
				//Message message = new Message(msgType.PRINTFILEDATA, dataINeed);
				m.fileData = dataINeed;
				SendMessageToClient(m);

				break;
			}
		}

		// client.DealWithMessage(new Message(msgType.PRINTFILEDATA,
		// fileMetaData));

	}

	/**
	 * @param metadata
	 */
	public void AddNewBlankChunk(Message message) {
		// TODO: have to create new Chunkmetadata and copy over metadata
		try{
			chunkMap.put(message.chunkClass.chunkHash, message.chunkClass);
			TFSFile current = file_list.get(1);
			message.chunkClass.byteoffset = current.spaceOccupied;

			message.chunkClass.size = 4;

			String s = "popo";
			byte buf[] = s.getBytes();	
			for (int i = 1; i <= s.length(); i++)
				file_list.get(1).data[current.spaceOccupied+ i] = buf[i-1];
			current.spaceOccupied += s.length();

			WritePersistentServerNodeMap(message.chunkClass.chunkHash, message.chunkClass);

			//WritePersistentServerFileData();
		}
		catch(Exception e){
			System.out.println("toobad");
			e.printStackTrace();
		}
		Message m = new Message(msgType.CREATEDIRECTORY, myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort);
		m.success = msgSuccess.REQUESTSUCCESS;
		SendMessageToMaster(m);
		//master.DealWithMessage(newMessage);


	}


	/**
	 * @param metadata
	 * @param byteArray
	 */
	public void AppendToFile(ChunkMetadata metadata, byte[] byteArray) {

		TFSFile current = new TFSFile(0); // TODO: remove hardcoding
		//Get the corresponding file number
		for(TFSFile tf:file_list){
			if(tf.fileNumber == metadata.filenumber)
				current = tf;
		}
		System.out.println("Available file byte size: "+(current.data.length-current.spaceOccupied));
		System.out.println("File #: "+current.fileNumber);
		System.out.println("Metadata correct file #: "+metadata.filenumber);
		ByteBuffer.allocate(4).putInt(metadata.size).array();
		byte[] fourBytesBefore = ByteBuffer.allocate(4).putInt(metadata.size).array();
		for(int i=0;i<4;i++){
			current.data[current.spaceOccupied] = fourBytesBefore[i];
			current.spaceOccupied++;
		}
		System.out.println("occupied length: "+current.spaceOccupied);
		System.out.println("add length: "+byteArray.length);

		metadata.byteoffset = current.spaceOccupied;
		metadata.size = byteArray.length;


		for(int i=0;i<byteArray.length;i++){
			current.data[current.spaceOccupied] = byteArray[i];
			current.spaceOccupied++;
		}

		//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		//		try { // appends byteArray to the end of current.data
		//			
		//			outputStream.write(current.data);
		//			System.out.println(" output stream size is "+outputStream.size()+" now adding the new data");
		//			outputStream.write(byteArray);
		//			current.spaceOccupied+=byteArray.length;
		//			System.out.println(" done.output stream size is now "+outputStream.size());
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}
		//		current.data = outputStream.toByteArray(); // does this create a new
		//													// correct file or append(it
		//													// shouldn't append)
		//		current.spaceOccupied+=byteArray.length;
		byte[] fourBytesAfter = ByteBuffer.allocate(4).putInt(metadata.size).array();
		for(int i=0;i<4;i++){
			current.data[current.spaceOccupied] = fourBytesAfter[i];
			current.spaceOccupied++;
		}
		//		current.spaceOccupied = current.data.length;

		//		try {
		//			String decoded;
		//			decoded = new String(Arrays.copyOfRange(current.data, 1, 19), "UTF-8");
		//			System.out.println("String reads "+decoded+" -spacedoccupied is "+current.spaceOccupied );
		//			
		//		} catch (UnsupportedEncodingException e1) {
		//			// TODO Auto-generated catch block
		//			e1.printStackTrace();
		//		}

		chunkMap.put(metadata.chunkHash, metadata);

		Message m = new Message(msgType.APPENDTOFILE, myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort);
		m.success = msgSuccess.REQUESTSUCCESS;

		//appending on
		WritePersistentServerNodeMap(metadata.chunkHash,metadata);
		WriteDataToFile(current, byteArray/*current.data*/);
		SendMessageToMaster(m);
		//master.DealWithMessage(newMessage);
	}

	public void WriteToNewFile(Message message) {

		TFSFile current = new TFSFile(message.chunkClass.filenumber);
		//Get the corresponding file number
		for(TFSFile tf:file_list){
			if(tf.fileNumber == message.chunkClass.filenumber)
				current = tf;
		}
		System.out.println("Available file byte size: "+(current.data.length-current.spaceOccupied));
		System.out.println("File #: "+current.fileNumber);
		System.out.println("Metadata correct file #: "+message.chunkClass.filenumber);
		ByteBuffer.allocate(4).putInt(message.chunkClass.size).array();
		byte[] fourBytesBefore = ByteBuffer.allocate(4).putInt(message.chunkClass.size).array();
		for(int i=0;i<4;i++){
			current.data[current.spaceOccupied] = fourBytesBefore[i];
			current.spaceOccupied++;
		}
		System.out.println("occupied length: "+current.spaceOccupied);
		System.out.println("add length: "+message.fileData.length);

		message.chunkClass.byteoffset = current.spaceOccupied;
		message.chunkClass.size = message.fileData.length;


		for(int i=0;i<message.fileData.length;i++){
			current.data[current.spaceOccupied] = message.fileData[i];
			current.spaceOccupied++;
		}

		byte[] fourBytesAfter = ByteBuffer.allocate(4).putInt(message.chunkClass.size).array();
		for(int i=0;i<4;i++){
			current.data[current.spaceOccupied] = fourBytesAfter[i];
			current.spaceOccupied++;
		}


		chunkMap.put(message.chunkClass.chunkHash, message.chunkClass);

		Message m = new Message(msgType.WRITETONEWFILE, myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort);
		m.chunkClass = message.chunkClass;
		m.success = msgSuccess.REQUESTSUCCESS;

		//appending on
		WritePersistentServerNodeMap(message.chunkClass.chunkHash,message.chunkClass);
		WriteDataToFile(current, current.data);
		SendMessageToMaster(m);
		//master.DealWithMessage(newMessage);
	}

	/**
	 * @param metadata
	 */
	public void DeleteChunk(ChunkMetadata metadata) {
		String chunkToDelete = null;
		for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
		{
			//System.out.println(entry.getValue().filename + " " + metadata.filename);
			if(entry.getValue().chunkHash == metadata.chunkHash)
			{
				for(TFSFile f: file_list)
				{
					//System.out.println(entry.getValue().filenumber + " " + f.fileNumber);
					if(f.fileNumber == entry.getValue().filenumber)
					{
						for(int i=0;i<entry.getValue().size;i++)
						{
							f.data[i+entry.getValue().byteoffset] = 0; //TODO:need to change later
						}
						f.spaceOccupied -= entry.getValue().size;
					}
				}
				chunkToDelete = entry.getKey();

				Message successMessageToMaster = new Message(msgType.DELETEDIRECTORY, myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort);
				successMessageToMaster.success = msgSuccess.REQUESTSUCCESS;
				SendMessageToMaster(successMessageToMaster);

				break;
			}
		}
		if (chunkToDelete != null) {
			chunkMap.remove(chunkToDelete);

			ClearChunkMap();
			for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
			{
				WritePersistentServerNodeMap(entry.getKey(),entry.getValue());
			}
		}
	}

	/**
	 * @param metadata
	 */
	public void CountNumInFile(ChunkMetadata metadata)
	{
		int numCounted = 0;
		String chunkSizeString = null;
		int chunkSize;

		for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
		{
			if(entry.getValue().filename == metadata.filename)
			{//Find the ChunkMetadata
				for(TFSFile f: file_list)
				{
					if(f.fileNumber == entry.getValue().filenumber)
					{
						for(int i = 0;i<f.data.length; )
						{
							for(int j=i;j<4+i;j++)
							{
								chunkSizeString += (char)f.data[j]; //possibly should be byte
							}
							chunkSize = Integer.parseInt(chunkSizeString);
							i += chunkSize;
							i += 8; // to discard the 2 4-byte size storage at beginning & end
							numCounted++;
						}

						Message successMessageToMaster = new Message(msgType.COUNTFILES, myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort);
						successMessageToMaster.success = msgSuccess.REQUESTSUCCESS;
						successMessageToMaster.countedLogicalFiles = numCounted;
						successMessageToMaster.filePath = metadata.filename;
						SendMessageToMaster(successMessageToMaster);
						//master.DealWithMessage(successMessageToMaster);
						break;
					}
				}
				break;
			}
		}

	}

	/**
	 * @param message
	 */
	void AppendToTFSFile(Message message) { // Test 6
		try {
			ChunkMetadata metadata = message.chunkClass;
			byte[] byteArray = message.fileData;
			TFSFile current =  file_list.get(metadata.filenumber);
			System.out.println("Available file byte size: "+(current.data.length-current.spaceOccupied));
			System.out.println("File #: "+current.fileNumber);
			System.out.println("Metadata correct file #: "+metadata.filenumber);
			ByteBuffer.allocate(4).putInt(metadata.size).array();
			byte[] fourBytesBefore = ByteBuffer.allocate(4).putInt(metadata.size).array();
			for(int i=0;i<4;i++){
				current.data[current.spaceOccupied] = fourBytesBefore[i];
				current.spaceOccupied++;
			}

			metadata.byteoffset = current.spaceOccupied;
			metadata.size = byteArray.length;

			for(int i=0;i<byteArray.length;i++){
				current.data[current.spaceOccupied] = byteArray[i];
				current.spaceOccupied++;
			}

			byte[] fourBytesAfter = ByteBuffer.allocate(4).putInt(metadata.size).array();
			for(int i=0;i<4;i++){
				current.data[current.spaceOccupied] = fourBytesAfter[i];
				current.spaceOccupied++;
			}			
			System.out.println("occupied length: "+current.spaceOccupied);
			System.out.println("add length: "+byteArray.length);
			chunkMap.put(metadata.chunkHash, metadata);

			Message m = new Message(msgType.APPENDTOTFSFILE, myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort);

			m.success = msgSuccess.REQUESTSUCCESS;
			m.chunkClass = metadata;

			//appending on
			WritePersistentServerNodeMap(metadata.chunkHash,metadata);
			WriteDataToFile(current, byteArray);
			SendMessageToMaster(m);
			//master.DealWithMessage(newMessage);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.out.println("AppendToTFSFile failed");
		}
	}

	/////////////////////////////WRITING TO PERSISTENT DATA///////////////////////////

	/**
	 * 
	 */
	public void ClearChunkMap() {
		BufferedWriter out = null;
		try {
			File file = new File("dataStorage/SData_ChunkMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false); // true
			// tells
			// to
			// append
			// data.
			out = new BufferedWriter(fstream);
			//System.out.println("Writing out to file");
			out.write("");
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
		finally
		{
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void LoadServerNodeMap() {
		String path = "dataStorage/SData_ChunkMap.txt";
		BufferedReader textReader = null;
		try {
			FileReader fr = new FileReader(path);
			textReader = new BufferedReader(fr);
			String textLine;

			while ((textLine = textReader.readLine()) != null) {
				// STRUCTURE///
				// KEY VERSION# SIZEOF_LOCATIONLIST
				// CHUNKLOCATION1_IP CHUNKLOCATION1_PORT ... CHUNKLOCATIONN_IP
				// CHUNKLOCATIONN_PORT
				// CHUNKHASH
				// REFERENCECOUNT
				// FILENAME
				// FILENUMBER
				// BYTEOFFSET
				// INDEX
				// SIZE
				String[] data = textLine.split("\t");

				// key
				String key;
				key = data[0];

				// version
				int n_version = Integer.parseInt(data[1]);

				// location
				List<ChunkLocation> locations = new ArrayList<ChunkLocation>();
				int locationSize = Integer.parseInt(data[2]);
				int newIndexCounter = 3 + (locationSize / 2);
				for (int i = 3; i < newIndexCounter; i = i + 2) {
					locations.add(new ChunkLocation(data[i], Integer
							.parseInt(data[i + 1])));
				}

				// hash
				/*
				 * List<Integer> hash = new ArrayList<Integer>(); String
				 * n_tempHash = data[newIndexCounter++]; for(int
				 * i=0;i<n_tempHash.length();i++) {
				 * hash.add(Character.getNumericValue
				 * (n_tempHash.charAt(i)));//adds at end } n_tempHash =
				 * hash.toString();
				 */

				// hash
				String n_hash = data[newIndexCounter++];

				// count
				int n_count = Integer.parseInt(data[newIndexCounter++]);

				// filename
				String n_fileName = data[newIndexCounter++];

				// fileNumber
				int n_fileNumber = Integer.parseInt(data[newIndexCounter++]);

				// byteoffset
				int n_byteOffset = Integer.parseInt(data[newIndexCounter++]);

				// index
				int n_index = Integer.parseInt(data[newIndexCounter++]);

				// size
				int n_size = Integer.parseInt(data[newIndexCounter++]);

				ChunkMetadata newMetaData = new ChunkMetadata(n_fileName,
						n_index, n_version, n_count);
				newMetaData.listOfLocations = locations;

				newMetaData.chunkHash = n_fileName + n_index;

				newMetaData.filenumber = n_fileNumber;
				newMetaData.byteoffset = n_byteOffset;
				newMetaData.size = n_size;
				chunkMap.put(key, newMetaData);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try {
				textReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void LoadFileData()
	{
		for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet()) {		
			TFSFile fileToStoreInto = file_list.get(entry.getValue().filenumber);
			String path = "dataStorage/File" + entry.getValue().filenumber;

			try {
				Path path1 = Paths.get(path);
				byte[] testData = new byte[entry.getValue().size+8];
				testData = Files.readAllBytes(path1);
				byte[] fileSize = new byte[4];
				for(int i = 0; i<4;i++)
				{
					fileSize[i] = testData[entry.getValue().byteoffset + i];
				}
				fileToStoreInto.spaceOccupied = java.nio.ByteBuffer.wrap(fileSize).getInt();
				byte[] data = new byte[entry.getValue().size];
				for(int i = 4; i<entry.getValue().size-4;i++)
				{
					data[i-4] = testData[entry.getValue().byteoffset+i];
				}
				fileToStoreInto.data = data;


			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param key
	 * @param chunkmd
	 */
	public void WritePersistentServerNodeMap(String key, ChunkMetadata chunkmd)
	{
		//String fileToWriteTo = "dataStorage/File" + chunkmd.filenumber;
		//STRUCTURE///
		//KEY VERSION# SIZEOF_LOCATIONLIST 
		//CHUNKLOCATION1_IP CHUNKLOCATION1_PORT ... CHUNKLOCATIONN_IP CHUNKLOCATIONN_PORT
		//CHUNKHASH
		//REFERENCECOUNT
		//FILENAME
		//FILENUMBER
		//BYTEOFFSET
		//INDEX
		//SIZE
		BufferedWriter out = null;
		File file = null;
		FileWriter fstream = null;
		try  
		{
			file = new File("dataStorage/SData_ChunkMap.txt");
			fstream = new FileWriter(file.getAbsoluteFile(), true); //true tells to append data.

			out = new BufferedWriter(fstream);
			out.write(key + "\t" + chunkmd.versionNumber + "\t"
					+ chunkmd.listOfLocations.size() + "\t");
			for (int i = 0; i < chunkmd.listOfLocations.size(); i++) {
				out.write(chunkmd.listOfLocations.get(i).chunkIP + "\t"
						+ chunkmd.listOfLocations.get(i).chunkPort + "\t");
			}
			out.write(chunkmd.chunkHash + "\t" + chunkmd.referenceCount + "\t"
					+ chunkmd.filename + "\t");
			out.write(chunkmd.filenumber + "\t" + chunkmd.byteoffset + "\t"
					+ chunkmd.index + "\t" + chunkmd.size);
			out.newLine();
		}
		catch (IOException e)
		{
			System.err.println("Error: " + e.getMessage());
		}
		finally
		{
			try {
				out.close();
				fstream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * @param file
	 * @param data
	 */
	public void WriteDataToFile(TFSFile file, byte[] data)
	{
		//BufferedWriter out = null;
		OutputStream os = null;
		try{
			os = new FileOutputStream(new File("dataStorage/File" + file.fileNumber),true);//"dataStorage/File"+file.fileNumber+".txt"));
			os.write(ByteBuffer.allocate(4).putInt(file.spaceOccupied).array());
			os.write(data);
			os.write(ByteBuffer.allocate(4).putInt(file.spaceOccupied).array());
		}
		catch (IOException e)
		{
			System.err.println("Error: " + e.getMessage());
		}
		finally
		{
			try {
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * TODO: Sends ping to Master telling it it's still alive and kicking
	 */
	public void PingMaster (HeartBeat ping){
		//HeartBeat ping = new HeartBeat(myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort, serverStatus.ALIVE);
		SendMessageToMaster(ping);
		//master.DealWithMessage(ping);
	}
	////////PROCEDURE FOR BRINGING A CHUNKSERVER BACK UP ////////////////////////////////////
	//Master sends information to check version numbrer
	//This chunkserver sends message to another chunkserver to send data if out of date
	//Other chunkserver sends data over to this chunkserver.

	/**
	 * 
	 * @param msg
	 */
	public void CheckVersionAfterStarting(SOSMessage msg) //MESSAGE THAT COMES FROM MASTER TO CHECK VERSION NUMBER
	{ 
		for(Map.Entry<String, ChunkMetadata> cmEntry : chunkMap.entrySet())
		{
			if(cmEntry.getValue().chunkHash == msg.chunkClass.chunkHash && 
					cmEntry.getValue().versionNumber < msg.chunkClass.versionNumber)
			{
				//TODO: Message to Master to get new data
				msg.msgToServer = msgTypeToServer.TO_OTHERSERVER;
				SendMessageToMaster(msg);
				return;
			}
		}
	}

	public void SendingDataToUpdateChunkServer(SOSMessage msg)
	{
		for(TFSFile file: file_list)
		{
			if(file.fileNumber == msg.chunkClass.filenumber)
			{
				//TODO: fix later with change in byteoffset variable
				for(int i=0; i<msg.chunkClass.size; i++)
				{
					msg.fileData[i] = file.data[msg.chunkClass.byteoffset + i];
				}
				msg.receiverIP = msg.SOSserver;
				msg.msgToServer = msgTypeToServer.RECEIVINGDATA;
				SendMessageToChunkServer(msg);
			}
		}
	}

	public void ReplacingData(SOSMessage msg) //MESSAGE THAT COMES FROM CHUNKSERVER TO GIVE DATA
	{
		for(Map.Entry<String, ChunkMetadata> cmEntry : chunkMap.entrySet())
		{
			if(cmEntry.getValue().chunkHash == msg.chunkClass.chunkHash && 
					cmEntry.getValue().versionNumber < msg.chunkClass.versionNumber)
			{
				//removing from the chunkmap and adding in the correct information
				chunkMap.remove(cmEntry.getKey());
				chunkMap.put(msg.chunkClass.chunkHash, msg.chunkClass);

				for(TFSFile file: file_list)
				{
					if(file.fileNumber == msg.chunkClass.filenumber)
					{
						for(int i=0;i<msg.chunkClass.size;i++)
						{
							file.data[msg.chunkClass.byteoffset+i] = msg.fileData[i];
						}
						file.spaceOccupied -= msg.chunkClass.size;

						String path = "dataStorage/File" + file.fileNumber;
						OutputStream os = null;
						try {
							Path path1 = Paths.get(path);
							byte[] testData = new byte[file.data.length + msg.chunkClass.size +8]; //CHECK IF CORRECT SIZE
							testData = Files.readAllBytes(path1);

							for (int i=0; i<4;i++)
							{
								testData[msg.chunkClass.byteoffset - 4 + i] = ByteBuffer.allocate(4).putInt(msg.chunkClass.size).array()[i];	
							}
							for (int i=0;i<msg.chunkClass.size;i++)
							{
								testData[msg.chunkClass.byteoffset + i] = file.data[msg.chunkClass.byteoffset + i];
							}
							for (int i=0; i<4;i++)
							{
								testData[msg.chunkClass.byteoffset + msg.chunkClass.size + i] = ByteBuffer.allocate(4).putInt(msg.chunkClass.size).array()[i];	
							}

							os = new FileOutputStream(new File("dataStorage/File" + file.fileNumber));//"dataStorage/File"+file.fileNumber+".txt"));
							os.write(testData);
						}
						catch (IOException e)
						{
							System.err.println("Error: " + e.getMessage());
						}
						finally
						{
							try {
								os.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

					}
				}
				SendMessageToMaster(msg);
				return;
			}
		}

	}

	/**
	 * @param message
	 */
	public void SendMessageToChunkServer(Message message) {
		SendMessage(message);
	}


	/**
	 * @param message
	 */
	public void SendMessageToClient(Message message) {
		SendMessage(message);
	}

	/**
	 * @param message
	 */
	public void SendMessageToMaster(Message message) {
		SendMessage(message);
	}

}
