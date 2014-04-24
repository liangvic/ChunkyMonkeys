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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.HeartBeat;
import Utility.HeartBeat.serverStatus;
import Utility.Message;
import Utility.NamespaceNode;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;

public class ChunkServerNode extends ServerNode {
	public ClientServerNode client;
	public MasterServerNode master;

	public class TFSFile {
		int fileNumber = 1;
		int spaceOccupied = 0;
		byte[] data = new byte[67108864];
		
		public TFSFile(int num){
			fileNumber = num;
			
		}
	}

	List<TFSFile> file_list = new ArrayList<TFSFile>();
	


	public ChunkServerNode() {
		for (int i = 0; i <= 4; i++){
			file_list.add(new TFSFile(i));
		}
		
		LoadServerNodeMap();
		LoadFileData();
	}

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
	public void WILLBEMAIN() throws Exception {	
		try (ServerSocket serverSocket = new ServerSocket(myPortNumber);)

		{
			while(true) { 
				Socket clientSocket = serverSocket.accept();
				ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				Message incoming = (Message)in.readObject();
				//TODO: put messages in queue
				DealWithMessage(incoming);
				//outToClient.writeBytes(capitalizedSentence); 
			}

			//TODO: Put in timer to increase TTL and check on status of all servers in ServerMap
			//TODO: Deal with Server Pings
			//TODO: Send updated chunkserver data to re-connected servers
		}
		catch (IOException e) {
			System.out
			.println("Exception caught when trying to listen on port "
					+ myPortNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
		finally{

		}
	}

	/**
	 * @param message
	 */
	public void DealWithMessage(Message message) {
		if (message.type == msgType.DELETEDIRECTORY) {
			DeleteChunk(message.chunkClass);
		}

		else if (message.type == msgType.CREATEFILE) {
			AddNewBlankChunk(message.chunkClass);
		} else if (message.type == msgType.READFILE) {
			ReadChunks(message.chunkClass);
		} else if (message.type == msgType.APPENDTOFILE) {
			if (message.chunkClass == null) {
				System.out.println("chunkClass is null");
			}
			else
				AppendToFile(message.chunkClass, message.fileData);
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
				WriteToNewFile(message.chunkClass, message.fileData);
		}
	}

	/**
	 * @param metadata
	 */
	public void ReadChunks(ChunkMetadata metadata){
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
			System.out.println("ChunkServer: Looking at file "+fileData.fileNumber + " looking for file " + metadata.filenumber);
			if(metadata.filenumber == fileData.fileNumber){
				System.out.println("ChunkServer: Available free byte size: "+(fileData.data.length-fileData.spaceOccupied));
				System.out.println("ChunkServer: Reading from file number "+metadata.filenumber);
				System.out.println("ChunkServer: Reading array size is "+metadata.size +" with byteoffset: "+metadata.byteoffset);
				System.out.println("ChunkServer: File data occupied space: "+fileData.spaceOccupied);
				
				
				byte[] dataINeed = new byte[metadata.size+4];
				// check byte offset
				int offSetIndex = metadata.byteoffset;
				for (int i = 0; i < metadata.size; i++) {
					dataINeed[i] = fileData.data[offSetIndex];
					offSetIndex++;
				}
				
				
				
				client.DealWithMessage(new Message(msgType.PRINTFILEDATA ,dataINeed));

				break;
			}
		}

		// client.DealWithMessage(new Message(msgType.PRINTFILEDATA,
		// fileMetaData));

	}

	/**
	 * @param metadata
	 */
	public void AddNewBlankChunk(ChunkMetadata metadata) {
		// TODO: have to create new Chunkmetadata and copy over metadata
		try{
		chunkMap.put(metadata.chunkHash, metadata);
		TFSFile current = file_list.get(1);
		metadata.byteoffset = current.spaceOccupied;

		metadata.size = 4;
		
		String s = "popo";
		byte buf[] = s.getBytes();	
		for (int i = 1; i <= s.length(); i++)
			file_list.get(1).data[current.spaceOccupied+ i] = buf[i-1];
		current.spaceOccupied += s.length();

		WritePersistentServerNodeMap(metadata.chunkHash, metadata);

		//WritePersistentServerFileData();
		}
		catch(Exception e){
			System.out.println("toobad");
			e.printStackTrace();
		}
		Message newMessage = new Message(msgType.CREATEDIRECTORY, metadata);
		newMessage.success = msgSuccess.REQUESTSUCCESS;
		master.DealWithMessage(newMessage);

		
	}


	/**
	 * @param metadata
	 * @param byteArray
	 */
	public void AppendToFile(ChunkMetadata metadata, byte[] byteArray) {

		TFSFile current = new TFSFile(0);
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
		
		Message newMessage = new Message(msgType.APPENDTOFILE, metadata);
		newMessage.success = msgSuccess.REQUESTSUCCESS;
		newMessage.addressedTo = serverType.MASTER;
		newMessage.sender = serverType.CHUNKSERVER;
		
		//appending on
		WritePersistentServerNodeMap(metadata.chunkHash,metadata);
		WriteDataToFile(current, byteArray/*current.data*/);
		master.DealWithMessage(newMessage);
	}

	public void WriteToNewFile(ChunkMetadata metadata, byte[] byteArray) {

		TFSFile current = new TFSFile(0);
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
		
		byte[] fourBytesAfter = ByteBuffer.allocate(4).putInt(metadata.size).array();
		for(int i=0;i<4;i++){
			current.data[current.spaceOccupied] = fourBytesAfter[i];
			current.spaceOccupied++;
		}

		
		chunkMap.put(metadata.chunkHash, metadata);
		
		Message newMessage = new Message(msgType.WRITETONEWFILE, metadata);
		newMessage.success = msgSuccess.REQUESTSUCCESS;
		newMessage.addressedTo = serverType.MASTER;
		newMessage.sender = serverType.CHUNKSERVER;
		
		//appending on
		WritePersistentServerNodeMap(metadata.chunkHash,metadata);
		WriteDataToFile(current, current.data);
		master.DealWithMessage(newMessage);
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
    			
    			Message successMessageToMaster = new Message(msgType.DELETEDIRECTORY);
        		successMessageToMaster.success = msgSuccess.REQUESTSUCCESS;
        		master.DealWithMessage(successMessageToMaster);
        		
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
						
						Message successMessageToMaster = new Message(msgType.COUNTFILES);
						successMessageToMaster.success = msgSuccess.REQUESTSUCCESS;
						successMessageToMaster.countedLogicalFiles = numCounted;
						successMessageToMaster.filePath = metadata.filename;
						master.DealWithMessage(successMessageToMaster);
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
			
			Message newMessage = new Message(msgType.APPENDTOTFSFILE, metadata);
			newMessage.success = msgSuccess.REQUESTSUCCESS;
			newMessage.addressedTo = serverType.MASTER;
			newMessage.sender = serverType.CHUNKSERVER;
			
			//appending on
			WritePersistentServerNodeMap(metadata.chunkHash,metadata);
			WriteDataToFile(current, byteArray);
			master.DealWithMessage(newMessage);
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
	public void PingMaster (){
		HeartBeat ping = new HeartBeat(myIP, serverType.CHUNKSERVER, serverStatus.ALIVE);
		master.DealWithMessage(ping);
	}
}
