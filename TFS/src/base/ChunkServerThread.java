package base;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Config;
import Utility.HeartBeat;
import Utility.HeartBeat.serverStatus;
import Utility.Message;
import Utility.SOSMessage;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.SOSMessage.msgTypeToServer;
import Utility.TFSFile;

public class ChunkServerThread extends ServerThread {
	ChunkServerNode server;

	List<TFSFile> file_list;
	// hash to data
	Map<String, ChunkMetadata> chunkMap;

	String myIP;
	int myInputPortNumber; 
	serverType myType;
	int masterPort;
	String masterIP;
	Semaphore fileWriteSemaphore = new Semaphore(1,true);
	Semaphore chunkMapSemaphore = new Semaphore(1,true);
	Semaphore appendToFileSemaphore = new Semaphore(1,true);

	public ChunkServerThread(ChunkServerNode sn, Socket s) {
		super(sn, s);
		server = sn;
		file_list = server.file_list;
		chunkMap = server.chunkMap;
		myIP = server.myIP;
		myType = serverType.CHUNKSERVER;
		masterIP = Config.prop.getProperty("MASTERIP");
		masterPort = Integer.parseInt(Config.prop.getProperty("MASTER_INPORT"));
		//PingMaster();
	}

	public void DealWithMessage(Message message) {
		//if(!messageList.isEmpty()) {
		//	Message message = messageList.get(0);
		System.out.println("Chunkserve: I GOT MESSAGE. Type = "+message.type.toString());
		if(message instanceof HeartBeat)
		{
			PingMaster((HeartBeat)message);
		}
		else if(message instanceof SOSMessage)
		{
			if(((SOSMessage) message).msgToServer == msgTypeToServer.TO_SOSSERVER)
			{
				CheckVersionAfterStarting((SOSMessage)message);
			}
			else if (((SOSMessage) message).msgToServer == msgTypeToServer.TO_OTHERSERVER)
			{
				SendingDataToUpdateChunkServer((SOSMessage)message);
			}
			else if (((SOSMessage) message).msgToServer == msgTypeToServer.RECEIVINGDATA)
			{
				ReplacingData((SOSMessage)message);
			}
		}
		else if (message.type == msgType.DELETEDIRECTORY) {
			DeleteChunk(message.chunkClass);
		}

		else if (message.type == msgType.CREATEFILE) {
			AddNewBlankChunk(message);
		} else if (message.type == msgType.READFILE) {
			ReadChunks(message);
		} else if (message.type == msgType.APPENDTOFILE) {
			if (message.chunkClass == null) {
				System.out.println("chunkClass is null");
			}
		}
		else if (message.type == msgType.CREATEFILE) {
			AddNewBlankChunk(message);
		} else if (message.type == msgType.READFILE) {
			ReadChunks(message);
		} else if (message.type == msgType.APPENDTOTFSFILE) {
			if(message.sender == serverType.MASTER) {
				System.out.println("Putting "+message.chunkClass.chunkHash+" into the map");
				chunkMap.put(message.chunkClass.chunkHash, message.chunkClass);
			}
			else if (message.sender == serverType.CLIENT) {
				System.out.println("Calling AppendToTSFFile Method");
				AppendToTFSFile(message);
			}
		} else if (message.type == msgType.COUNTFILES) {
			CountNumInFile(message.chunkClass);
		}
		else if (message.type == msgType.WRITETONEWFILE)
		{
			if (message.chunkClass == null) {
				System.out.println("chunkClass is null");
			}
			else
				WriteToNewFile(message);
		}

		server.messageList.remove(message);

	}
	//}

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
		synchronized(file_list)
		{
			int offSetIndex = -1;
			
			ChunkMetadata current = message.chunkClass;
			for (ChunkLocation a : current.listOfLocations){
				if (a.chunkIP == myIP && a.chunkPort == myInputPortNumber){
					offSetIndex = a.byteOffset;
				}
			}
			
			for(TFSFile fileData:file_list){
				System.out.println("ChunkServer: Looking at file "+fileData.fileNumber + " looking for file " + message.chunkClass.filenumber);
				if(message.chunkClass.filenumber == fileData.fileNumber){
					System.out.println("ChunkServer: Available free byte size: "+(fileData.data.length-fileData.spaceOccupied));
					System.out.println("ChunkServer: Reading from file number "+message.chunkClass.filenumber);
					System.out.println("ChunkServer: Reading array size is "+message.chunkClass.size +" with byteoffset: "+ offSetIndex);
					System.out.println("ChunkServer: File data occupied space: "+fileData.spaceOccupied);


					byte[] dataINeed = new byte[message.chunkClass.size+4];
					// check byte offset
					
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
			int offSetIndex = -1;
			
			ChunkMetadata chunkmeta = message.chunkClass;
			ChunkLocation chunkloc = null;
			for (ChunkLocation a : chunkmeta.listOfLocations){
				if (a.chunkIP == myIP && a.chunkPort == myInputPortNumber){
					chunkloc = a;
				}
			}
			
			System.out.println("Adding a new blank chunk");
			chunkMap.put(message.chunkClass.chunkHash, message.chunkClass);
			TFSFile current = file_list.get(1);
			chunkloc.byteOffset = current.spaceOccupied;

			message.chunkClass.size = 4;

			String s = "popo";
			byte buf[] = s.getBytes();

			for (int i = 1; i <= s.length(); i++)
			{
				file_list.get(1).data[current.spaceOccupied+ i] = buf[i-1];
			}

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

	public void WriteToNewFile(Message message) {

		TFSFile current = new TFSFile(message.chunkClass.filenumber);
		//Get the corresponding file number
		synchronized(file_list)
		{
			for(TFSFile tf:file_list){
				if(tf.fileNumber == message.chunkClass.filenumber)
					current = tf;
			}
		}
		System.out.println("Available file byte size: "+(current.data.length-current.spaceOccupied));
		System.out.println("File #: "+current.fileNumber);
		System.out.println("Metadata correct file #: "+message.chunkClass.filenumber);
		//ByteBuffer.allocate(4).putInt(message.chunkClass.size).array();
		byte[] fourBytesBefore = ByteBuffer.allocate(4).putInt(message.chunkClass.size).array();
		for(int i=0;i<4;i++){
			current.data[current.spaceOccupied] = fourBytesBefore[i];
			current.spaceOccupied++;
		}
		System.out.println("occupied length: "+current.spaceOccupied);
		System.out.println("add length: "+message.fileData.length);

		ChunkMetadata chunkmeta = message.chunkClass;
		ChunkLocation chunkloc = null;
		for (ChunkLocation a : chunkmeta.listOfLocations){
			if (a.chunkIP == myIP && a.chunkPort == myInputPortNumber){
				chunkloc = a;
			}
		}
		
		chunkloc.byteOffset = current.spaceOccupied;
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
		System.out.println("Write to file!");

//		Message m = new Message(msgType.WRITETONEWFILE, myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort);
//		m.chunkClass = message.chunkClass;
//		m.success = msgSuccess.REQUESTSUCCESS;
//
//		//appending on
		WritePersistentServerNodeMap(message.chunkClass.chunkHash,message.chunkClass);
		WriteDataToFile(current, message.fileData);
//		SendMessageToMaster(m);
		//master.DealWithMessage(newMessage);
	}

	/**
	 * @param metadata
	 */
	public void DeleteChunk(ChunkMetadata metadata) {
		String chunkToDelete = null;
		synchronized(chunkMap)
		{
			for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
			{
				//System.out.println(entry.getValue().filename + " " + metadata.filename);
				if(entry.getValue().chunkHash == metadata.chunkHash)
				{
					synchronized(file_list)
					{
						for(TFSFile f: file_list)
						{
							//System.out.println(entry.getValue().filenumber + " " + f.fileNumber);
							if(f.fileNumber == entry.getValue().filenumber)
							{
								/*for(int i=0;i<entry.getValue().size;i++)
								{
									f.data[i+entry.getValue().byteOffset] = 0; //TODO:Need to change the information when there is nothing
								}*/
								f.spaceOccupied -= entry.getValue().size;
							}
						}
					}
					chunkToDelete = entry.getKey();

					Message successMessageToMaster = new Message(msgType.DELETEDIRECTORY, myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort);
					successMessageToMaster.success = msgSuccess.REQUESTSUCCESS;
					SendMessageToMaster(successMessageToMaster);

					break;
				}
			}
		}

		if (chunkToDelete != null) {
			chunkMap.remove(chunkToDelete);

			ClearChunkMap();
			synchronized(chunkMap)
			{
				for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
				{
					WritePersistentServerNodeMap(entry.getKey(),entry.getValue());
				}
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

		synchronized(chunkMap)
		{
			for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
			{
				if(entry.getValue().filename == metadata.filename)
				{//Find the ChunkMetadata
					synchronized(file_list)
					{
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
		}


	}

	/**
	 * @param message
	 */
	void AppendToTFSFile(Message message) { // Test 6
		appendToFileSemaphore.tryAcquire();
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

			ChunkMetadata chunkmeta = message.chunkClass;
			ChunkLocation chunkloc = null;
			for (ChunkLocation a : chunkmeta.listOfLocations){
				if (a.chunkIP == myIP && a.chunkPort == myInputPortNumber){
					chunkloc = a;
				}
			}
			
			chunkloc.byteOffset = current.spaceOccupied;
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
		finally
		{
			appendToFileSemaphore.release();
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
		chunkMapSemaphore.tryAcquire();
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
						+ chunkmd.listOfLocations.get(i).chunkPort + "\t"
						+ chunkmd.listOfLocations.get(i).byteOffset + "\t"
						+ chunkmd.listOfLocations.get(i).fileNumber + "\t");

			}
			out.write(chunkmd.chunkHash + "\t" + chunkmd.referenceCount + "\t"
					+ chunkmd.filename + "\t");
			/*out.write(chunkmd.filenumber + "\t" + chunkmd.byteOffset + "\t"
					+ chunkmd.index + "\t" + chunkmd.size);*/
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
			chunkMapSemaphore.release();
		}
	}

	/**
	 * @param file
	 * @param data
	 */
	public void WriteDataToFile(TFSFile file, byte[] data)
	{
		//BufferedWriter out = null;
		fileWriteSemaphore.tryAcquire();
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
			fileWriteSemaphore.release();
		}
	}


	/**
	 * TODO: Sends ping to Master telling it it's still alive and kicking
	 * @param message 
	 */
	public void PingMaster (HeartBeat message){
		HeartBeat ping = new HeartBeat(myIP, myType, myInputPortNumber, masterIP, serverType.MASTER, masterPort, serverStatus.ALIVE);
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
		synchronized(chunkMap)
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
	}

	public void SendingDataToUpdateChunkServer(SOSMessage msg)
	{
		synchronized(file_list)
		{
			for(TFSFile file: file_list)
			{
				if(file.fileNumber == msg.chunkClass.filenumber)
				{
					//TODO: fix later with change in byteoffset variable
					for(int i=0; i<msg.chunkClass.size; i++)
					{
						ChunkMetadata chunkmeta = msg.chunkClass;
						ChunkLocation chunkloc = null;
						for (ChunkLocation a : chunkmeta.listOfLocations){
							if (a.chunkIP == myIP && a.chunkPort == myInputPortNumber){
								chunkloc = a;
							}
						}
						msg.fileData[i] = file.data[chunkloc.byteOffset + i];
					}
					msg.receiverIP = msg.SOSserver;
					msg.msgToServer = msgTypeToServer.RECEIVINGDATA;
					SendMessageToChunkServer(msg);
				}
			}
		}

	}

	public void ReplacingData(SOSMessage msg) //MESSAGE THAT COMES FROM CHUNKSERVER TO GIVE DATA
	{
		synchronized(chunkMap)
		{
			for(Map.Entry<String, ChunkMetadata> cmEntry : chunkMap.entrySet())
			{
				if(cmEntry.getValue().chunkHash == msg.chunkClass.chunkHash && 
						cmEntry.getValue().versionNumber < msg.chunkClass.versionNumber)
				{
					//removing from the chunkmap and adding in the correct information
					chunkMap.remove(cmEntry.getKey());
					chunkMap.put(msg.chunkClass.chunkHash, msg.chunkClass);

					synchronized(file_list)
					{
						for(TFSFile file: file_list)
						{
							if(file.fileNumber == msg.chunkClass.filenumber)
							{
								ChunkMetadata chunkmeta = msg.chunkClass;
								ChunkLocation chunkloc = null;
								for (ChunkLocation a : chunkmeta.listOfLocations){
									if (a.chunkIP == myIP && a.chunkPort == myInputPortNumber){
										chunkloc = a;
									}
								}
								for(int i=0;i<msg.chunkClass.size;i++)
								{
									file.data[chunkloc.byteOffset+i] = msg.fileData[i];
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
										testData[chunkloc.byteOffset - 4 + i] = ByteBuffer.allocate(4).putInt(msg.chunkClass.size).array()[i];	
									}
									for (int i=0;i<msg.chunkClass.size;i++)
									{
										testData[chunkloc.byteOffset + i] = file.data[chunkloc.byteOffset + i];
									}
									for (int i=0; i<4;i++)
									{
										testData[chunkloc.byteOffset + msg.chunkClass.size + i] = ByteBuffer.allocate(4).putInt(msg.chunkClass.size).array()[i];	
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
		}

	}

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
	 * @param message
	 */
	public void SendMessageToChunkServer(Message message) {
		server.SendMessage(message);
	}


	/**
	 * @param message
	 */
	public void SendMessageToClient(Message message) {
		server.SendMessage(message);
	}

	/**
	 * @param message
	 */
	public void SendMessageToMaster(Message message) {
		server.SendMessage(message);
	}



}
