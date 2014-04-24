package base;

import java.io.*;
import java.net.*;
import java.util.*;

import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Config;
import Utility.HeartBeat;
import Utility.HeartBeat.serverStatus;
import Utility.Message;
import Utility.NamespaceNode.lockType;
import Utility.NamespaceNode.nodeType;
import Utility.TFSLogger;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.NamespaceNode;

public class MasterServerNode extends ServerNode {
	//public ClientServerNode client;
	//public ChunkServerNode chunkServer;

	int operationID = 0;
	// private static ServerSocket welcomeSocket;
	// chunkServerMap key is the filepath + chunk index
	Map<String, ChunkMetadata> chunkServerMap = new HashMap<String, ChunkMetadata>();
	Map<String, NamespaceNode> NamespaceMap = new HashMap<String, NamespaceNode>();
	Map<String, ServerData> ServerMap = new HashMap<String, ServerData>();
	TFSLogger tfsLogger = new TFSLogger();


	public class ServerData {
		String IP;
		int clientPort;
		int serverPort;
		int TTL; // time to last ping - the last time it pinged master
		serverStatus status;

		public ServerData(String nIP, int nClientPort, int nServerPort) {
			IP = nIP;
			clientPort = nClientPort;
			serverPort = nServerPort;
			status = null;
			TTL = 0;
		}
	}

	public MasterServerNode() {
		LoadChunkServerMap();
		LoadNamespaceMap();
		LoadServerData();
	}

	// Don't call on this for now; using monolith structure
	/**
	 * @throws Exception
	 */
	public void WILLBEMAIN() throws Exception {	
		try (ServerSocket serverSocket = new ServerSocket(myPortNumber);)

		{
			while(true) { 
				Socket clientSocket = serverSocket.accept();
				ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
				Message incoming = (Message)in.readObject();
				//TODO: put messages in queue
				DealWithMessage(incoming);
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
	 * @param inputMessage
	 */
	public void DealWithMessage(Message inputMessage) {
		operationID++; //used to differentiate operations
		System.out.println("inputMessagetype "+ inputMessage.type);
		if (inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CLIENT) {
			MDeleteDirectory(inputMessage.filePath,operationID);
		} else if (inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CHUNKSERVER) {
			RemoveParentLocks(inputMessage.filePath);
			if (inputMessage.success == msgSuccess.REQUESTSUCCESS) {
				// SendSuccessMessageToClient();
			} else {
				// SendErrorMessageToClient();
			}
		} else if (inputMessage.type == msgType.CREATEDIRECTORY) {
			if (inputMessage.sender == serverType.CLIENT) {
				try {
					CreateDirectory(inputMessage.filePath,operationID);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (inputMessage.sender == serverType.CHUNKSERVER) {
				RemoveParentLocks(inputMessage.filePath);
				if (inputMessage.success == msgSuccess.REQUESTSUCCESS) {
					// SendSuccessMessageToClient();
				} else {
					// SendErrorMessageToClient();
				}
			} else if (inputMessage.type == msgType.CREATEDIRECTORY) {
				if (inputMessage.sender == serverType.CLIENT) {
					try {
						CreateDirectory(inputMessage.filePath,operationID);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (inputMessage.sender == serverType.CHUNKSERVER) {
					if (inputMessage.success == msgSuccess.REQUESTSUCCESS) {
						System.out.println("Directory " + " creation successful");
					} else if (inputMessage.success == msgSuccess.REQUESTERROR) {
						System.out.println("Directory " + " creation failed");

					}
				}
			}
		} else if (inputMessage.type == msgType.CREATEFILE) {
			if (inputMessage.sender == serverType.CLIENT)
				CreateFile(inputMessage, operationID);
			else if (inputMessage.sender == serverType.CHUNKSERVER) {
				RemoveParentLocks(inputMessage.filePath);
				if (inputMessage.success == msgSuccess.REQUESTSUCCESS)
					System.out.println("File "
							+ inputMessage.chunkClass.filename
							+ " creation successful");
				else if (inputMessage.success == msgSuccess.REQUESTERROR)
					System.out.println("File "
							+ inputMessage.chunkClass.filename
							+ " creation failed");
			}
		} else if (inputMessage.type == msgType.READFILE) {
			if(inputMessage.sender == serverType.CLIENT)
			{
				ReadFile(inputMessage, operationID);
			}
			else if (inputMessage.sender == serverType.CHUNKSERVER)
			{
				RemoveParentLocks(inputMessage.filePath);
				//TODO: NEED TO ADD IN FURTHER IF STATEMENTS
			}
		}
		else if(inputMessage.type == msgType.APPENDTOFILE)
		{
			if(inputMessage.sender == serverType.CLIENT)
				AssignChunkServer(inputMessage);//, operationID);
			else if (inputMessage.sender == serverType.CHUNKSERVER){
				RemoveParentLocks(inputMessage.filePath);
				if(inputMessage.success == msgSuccess.REQUESTSUCCESS){
					System.out.println("File "+ inputMessage.chunkClass.filename + " creation successful");
				}
				else if (inputMessage.success == msgSuccess.REQUESTERROR)
					System.out.println("File " + inputMessage.chunkClass.filename + " creation failed");
			}
		}
		else if(inputMessage.type == msgType.APPENDTOTFSFILE) // Test 6
		{
			if(inputMessage.sender == serverType.CLIENT) {
				AppendToTFSFile(inputMessage, operationID);
			}
			else if(inputMessage.sender == serverType.CHUNKSERVER) {
				RemoveParentLocks(inputMessage.filePath);
				if(inputMessage.success == msgSuccess.REQUESTSUCCESS){
					System.out.println("File "+ inputMessage.chunkClass.filename + " append successful");
				}
			} else if (inputMessage.type == msgType.APPENDTOTFSFILE) // Test 6
			{

				FindFile(inputMessage.filePath, operationID);
			}
			else if (inputMessage.sender == serverType.CHUNKSERVER)
			{
				RemoveParentLocks(inputMessage.filePath);
				System.out.println("There are " + inputMessage.countedLogicalFiles + " logical files in " + inputMessage.filePath);
			}
		}else if(inputMessage.type == msgType.WRITETONEWFILE) // Test 4 & Unit 4
		{
			AssignChunkServer(inputMessage);
		}

	}
	/**
	 * 
	 * @param opID
	 */
	public void RemoveParentLocks(String filePath)
	{
		int opID = 0;
		if(NamespaceMap.containsKey(filePath))
		{
			opID = NamespaceMap.get(filePath).lockData.operationID;
		}

		for(Map.Entry<String, NamespaceNode> entry : NamespaceMap.entrySet())
		{
			//if this operation previously made the lock
			if(entry.getValue().lockData.operationID == opID)
			{
				entry.getValue().lockData.lockStatus = lockType.NONE;
			}
		}
	}

	/**
	 * 
	 * @param filePath
	 * @param opID
	 * @return
	 */
	public boolean AddExclusiveParentLocks(String filePath, int opID)
	{
		String[] tokens = filePath.split(File.pathSeparator);
		String parentPath = tokens[0];
		for(int i=1;i<tokens.length-1;i++)
		{
			if(NamespaceMap.containsKey(filePath))
			{
				if(NamespaceMap.get(parentPath).lockData.lockStatus == lockType.NONE)
				{
					if(parentPath != filePath)
					{
						NamespaceMap.get(parentPath).lockData.lockStatus = lockType.I_EXCLUSIVE;
						NamespaceMap.get(parentPath).lockData.operationID = opID;
					}
					else
					{
						NamespaceMap.get(parentPath).lockData.lockStatus = lockType.EXCLUSIVE;
						NamespaceMap.get(parentPath).lockData.operationID = opID;
					}
				}
				else if(NamespaceMap.get(parentPath).lockData.lockStatus == lockType.I_EXCLUSIVE ||
						NamespaceMap.get(parentPath).lockData.lockStatus == lockType.I_SHARED)
				{
					if(parentPath == filePath)
					{
						RemoveParentLocks(filePath);
						SendErrorMessageToClient(new Message(msgType.DELETEDIRECTORY, filePath));
						return false;
					}
					//if not the final node, allow it to pass
				}
				else if(NamespaceMap.get(parentPath).lockData.lockStatus == lockType.SHARED ||
						NamespaceMap.get(parentPath).lockData.lockStatus == lockType.EXCLUSIVE)
				{
					RemoveParentLocks(parentPath);
					SendErrorMessageToClient(new Message(msgType.DELETEDIRECTORY, filePath));
					return false;
				}
				parentPath = parentPath + "\\" + tokens[i]; 
			}
		}
		return true;
	}

	/**
	 * 
	 * @param filePath
	 * @param opID
	 * @return
	 */

	public boolean AddSharedParentLocks(String filePath, int opID)
	{
		String[] tokens = filePath.split(File.pathSeparator);
		String parentPath = tokens[0];
		for(int i=1;i<tokens.length-1;i++)
		{
			if(NamespaceMap.containsKey(filePath))
			{
				if(NamespaceMap.get(parentPath).lockData.lockStatus == lockType.NONE)
				{
					if(parentPath != filePath)
					{
						NamespaceMap.get(parentPath).lockData.lockStatus = lockType.I_SHARED;
						NamespaceMap.get(parentPath).lockData.operationID = opID;
					}
					else
					{
						NamespaceMap.get(parentPath).lockData.lockStatus = lockType.SHARED;
						NamespaceMap.get(parentPath).lockData.operationID = opID;
					}
				}
				else if(NamespaceMap.get(parentPath).lockData.lockStatus == lockType.I_EXCLUSIVE ||
						NamespaceMap.get(parentPath).lockData.lockStatus == lockType.I_SHARED)
				{
					if(parentPath == filePath)
					{
						RemoveParentLocks(filePath);
						return false;
					}
					//if not the final node, allow it to pass
				}
				else if(NamespaceMap.get(parentPath).lockData.lockStatus == lockType.EXCLUSIVE)
				{
					RemoveParentLocks(parentPath);
					return false;
				}
				parentPath = parentPath + "\\" + tokens[i]; 
			}
		}
		return true;
	}

	/**
	 * @param successMessage
	 */
	public void SendSuccessMessageToClient(Message successMessage) {
		successMessage.success = msgSuccess.REQUESTSUCCESS;
		SendMessageToClient(successMessage);
	}

	/** 
	 * @param errorMessage
	 */
	public void SendErrorMessageToClient(Message errorMessage) {
		errorMessage.success = msgSuccess.REQUESTERROR;
		SendMessageToClient(errorMessage);
	}

	/** 
	 * @param chunkServerMessage
	 */
	public void SendMessageToChunkServer(Message message) {
		//MESSAGE MUST HAVE IP and Socket Number		
		int port = ServerMap.get(message.senderIP).serverPort;	
		try(Socket serverSocket =  new Socket(message.senderIP, port);)
		{
			message.receiverIP = message.senderIP;
			message.addressedTo = serverType.CHUNKSERVER;
			message.sender = serverType.MASTER;
			message.senderIP = myIP;
			message.recieverPort = message.senderPort;
			message.senderPort = myPortNumber;
			ObjectOutputStream out = new ObjectOutputStream(serverSocket.getOutputStream());
			out.writeObject(message);
			out.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		finally{
		}
	}

	/** 
	 * @param clientServerMessage
	 */
	public void SendMessageToClient(Message message) {
		int port = ServerMap.get(message.senderIP).clientPort;	
		try(Socket clientSocket =  new Socket(message.senderIP, port);)
		{
			message.receiverIP = message.senderIP;
			message.addressedTo = serverType.CLIENT;
			message.sender = serverType.MASTER;
			message.senderIP = myIP;
			message.recieverPort = message.senderPort;
			message.senderPort = myPortNumber;
			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
			out.writeObject(message);
			out.close();
		}
		catch (IOException e){
			e.printStackTrace();
		}
		finally{
		}
	}

	/**
	 * 
	 * @param filePath
	 * @param opID
	 */
	public void MDeleteDirectory(String filePath, int opID) {
		if (NamespaceMap.containsKey(filePath)) {
			// now that have the node in the NamespaceTree, you iterate through
			// it's children

			if(AddExclusiveParentLocks(filePath, opID))
			{
				if (NamespaceMap.get(filePath).children.size() > 0) {
					// recursively going through the tree and deleting all
					// files/directories below
					deleteAllChildNodes(filePath);
				}

				String[] tokens = filePath.split(File.pathSeparator);
				String parentPath = tokens[0];
				for(int i=1;i<tokens.length-1;i++)
				{
					parentPath = parentPath + "\\" + tokens[i]; 
				}

				for (Map.Entry<String, NamespaceNode> entry : NamespaceMap.entrySet())
				{
					for(int i=0;i<entry.getValue().children.size();i++)
					{
						if(entry.getValue().children.get(i).contains(parentPath))
						{
							entry.getValue().children.remove(i);
						}
					}
				}

				// finally delete directory wanted to delete
				NamespaceMap.remove(filePath);

				tfsLogger.LogMsg("Deleted directory and all directories/files below " + filePath);

				ClearChunkServerMapFile();
				ClearNamespaceMapFile();

				for (Map.Entry<String, ChunkMetadata> entry : chunkServerMap.entrySet())
				{
					WritePersistentChunkServerMap(entry.getKey(),entry.getValue());
				}
				for (Map.Entry<String, NamespaceNode> entry : NamespaceMap.entrySet())
				{
					WritePersistentNamespaceMap(entry.getKey(),entry.getValue());
				}
				SendSuccessMessageToClient(new Message(msgType.DELETEDIRECTORY, filePath));
			}
			else
			{
				SendErrorMessageToClient(new Message(msgType.DELETEDIRECTORY, filePath));
				return;
			}
		}
	}

	/**
	 * @param startingNodeFilePath
	 */
	public void deleteAllChildNodes(String startingNodeFilePath) {
		if (NamespaceMap.get(startingNodeFilePath).children.size() == 0) {
			// initially start at chunk index 1
			int chunkIndex = 1;
			String chunkServerKey = startingNodeFilePath + chunkIndex;

			// Send message to client server to erase data IF IS FILE
			if (NamespaceMap.get(startingNodeFilePath).type == nodeType.FILE) {
				while (chunkServerMap.containsKey(chunkServerKey)) {
					// System.out.println("Going to delete the value");
					// sending protocol
					Message clientMessage = new Message(msgType.DELETEDIRECTORY);
					clientMessage.chunkClass = chunkServerMap
							.get(chunkServerKey);
					SendMessageToChunkServer(clientMessage);

					// delete the file from master's chunk server map
					chunkServerMap.remove(chunkServerKey);

					// increment for checking if there are more chunks
					chunkIndex++;
					chunkServerKey = startingNodeFilePath + chunkIndex;
				}
			}

			// remove the FILE or DIRECTORY from namespace map
			NamespaceMap.remove(startingNodeFilePath);
			// tfsLogger.LogMsg("Created file " + startingNodeFilePath);
			return;
		} else {
			for (int i = 0; i < NamespaceMap.get(startingNodeFilePath).children
					.size(); i++) {
				deleteAllChildNodes(NamespaceMap.get(startingNodeFilePath).children
						.get(i));
			}
			NamespaceMap.get(startingNodeFilePath).children.clear();
			NamespaceMap.remove(startingNodeFilePath);
		}
	}


	/**
	 * 
	 * @param inputMessage
	 * @param opID
	 */
	public void ReadFile(Message inputMessage, int opID) {
		if(AddSharedParentLocks(inputMessage.filePath, opID))
		{
			//Implement Later
			int indexCounter = 1;
			System.out.println("Master: trying to read "+inputMessage.filePath + indexCounter);
			if (!chunkServerMap.containsKey(inputMessage.filePath + indexCounter)) {
				System.out.println("Master: doesnt exist");
				SendErrorMessageToClient(new Message(msgType.READFILE, inputMessage.filePath));
				return;
			}

			// check if the file contains multiple chunk indexes
			indexCounter++;
			while (chunkServerMap.containsKey(inputMessage.filePath + indexCounter)) {
				//				ChunkMetadata cm = chunkServerMap.get(inputMessage.filePath + indexCounter);
				//				Message returnMessage = new Message(msgType.READFILE, cm);
				//				returnMessage.success = msgSuccess.REQUESTSUCCESS;
				//				System.out.println("Master: found chunk number "+indexCounter +" of file. its hash is "+cm.chunkHash);
				//				client.DealWithMessage(returnMessage);
				indexCounter++;
			}
			//Send client the number of chunk number to read
			//			client.ExpectChunkNumberForRead(indexCounter - 1);
			Message expectMsg = new Message(msgType.EXPECTEDNUMCHUNKREAD);
			expectMsg.success = msgSuccess.REQUESTSUCCESS;
			expectMsg.expectNumChunkForRead = indexCounter-1;
			SendMessageToClient(expectMsg);
			for(int i=1;i<indexCounter;i++){
				ChunkMetadata cm = chunkServerMap.get(inputMessage.filePath+ i);
				System.out.println("Master: first chunkhash is "+cm.chunkHash);
				Message returnMessage = new Message(msgType.READFILE, cm);
				returnMessage.success = msgSuccess.REQUESTSUCCESS;
				SendMessageToClient(returnMessage);
			}


		}
		else
		{
			SendErrorMessageToClient(new Message(msgType.READFILE, inputMessage.filePath));
		}
	}

	/**
	 * 
	 * @param inputMessage
	 * @return
	 */
	public void AssignChunkServer(Message inputMessage){//assign multiple chunk servers
		//TODO: NEED TO ADD IN THE LOCK CHECKING
		//if(AddExclusiveParentLocks(inputMessage.filePath, opID))
		//{
		List<ServerData> replicaList = new ArrayList<ServerData>();
		List<ServerData> allAvailableServerList = new ArrayList<ServerData>();
		String hashstring = inputMessage.filePath + "\\" + inputMessage.fileName + 1;

		if(inputMessage.type == msgType.WRITETONEWFILE)
		{
			ChunkMetadata testExistence = chunkServerMap.get(hashstring);
			if(testExistence != null)
			{
				//System.out.println("MSN AssignChunkServer: Filepath exists.");
				return;
			}
		}
		Random rand = new Random();
		int targetFileNumber = rand.nextInt(5);



		//do a check to see what the offset is


		//Get information about all chunkservers

		for(String ip:ServerMap.keySet()){
			allAvailableServerList.add(ServerMap.get(ip));
		}
		//Random replica assignment
		int chunkServerAssignment = 0;
		while(replicaList.size()<inputMessage.replicaCount){
			chunkServerAssignment = rand.nextInt(4);
			if(!replicaList.contains(allAvailableServerList.get(chunkServerAssignment)))
				replicaList.add(allAvailableServerList.get(chunkServerAssignment));
		}
		int[] replicaListLargestOffset = new int[replicaList.size()];
		Arrays.fill(replicaListLargestOffset, 0);
		//now were going to try to find the offset to write the new file
		//go through each of the chunk locations of all chunks

		for(String key: chunkServerMap.keySet()){
			for(ChunkLocation cl: chunkServerMap.get(key).listOfLocations){ //browsing all chunkserver locations
				if(cl.fileNumber == targetFileNumber){ //Same fileNumber
					//After finding correct filenumber, see if byteoffset is largest
					for(int n=0;n<replicaList.size();n++){ //Browsing all chosen replica servers for match
						if(cl.chunkIP == replicaList.get(n).IP){ //Same chunk server match with index n
							//							check if the same index n in largest byte array is actually the largest
							if(replicaListLargestOffset[n]<cl.byteOffset){
								replicaListLargestOffset[n] = cl.byteOffset+chunkServerMap.get(key).size+4;
							}
						}
					}
				}
			}
		}
		//Sending a create file for each replica
		for(int i = 0;i<replicaList.size();i++){

			ChunkMetadata newMetaData = new ChunkMetadata(inputMessage.fileName, 1,1,0);
			newMetaData.chunkHash = hashstring;
			newMetaData.filenumber = targetFileNumber;
			newMetaData.byteoffset = replicaListLargestOffset[i];
			newMetaData.size = inputMessage.fileData.length;
			//populate location
			List<ChunkLocation> newLocations = new ArrayList<ChunkLocation>();
			for(int j=0;j<replicaList.size();j++){
				ChunkLocation location = new ChunkLocation(replicaList.get(j).IP,replicaList.get(j).serverPort);
				location.fileNumber = targetFileNumber;
				location.byteOffset = replicaListLargestOffset[j];
				newLocations.add(location);
			}
			chunkServerMap.put(hashstring, newMetaData);
			inputMessage.chunkClass = newMetaData;
			inputMessage.addressedTo = serverType.CLIENT;
			inputMessage.sender = serverType.MASTER;
			SendMessageToClient(inputMessage);
		}
		//create a new namespace node
		//filename and get parent, add child.

		//============================Name Space Issues=========================================

		NamespaceNode nn = new NamespaceNode(nodeType.FILE);
		NamespaceMap.get(inputMessage.filePath).children.add(inputMessage.filePath + "\\" + inputMessage.fileName);
		//System.out.println("Got to the file");
		NamespaceMap.put(inputMessage.filePath + "\\" + inputMessage.fileName, nn);

		ClearNamespaceMapFile(); //need to clear so that correctly adds as child to parent directory
		//need to update children to, so have to clear and write again
		for (Map.Entry<String, NamespaceNode> entry : NamespaceMap.entrySet())
		{
			WritePersistentNamespaceMap(entry.getKey(),entry.getValue());
		}
		//only appending on
		WritePersistentChunkServerMap(hashstring,
				chunkServerMap.get(hashstring));

		//		Message metadataMsg = new Message(msgType.WRITETONEWFILE, newMetaData);


		//		return newMetaData;
		//client.AppendToChunkServer(hashstring, myServer);
		//client.AppendToChunkServer(newMetaData, chunkServer);
		/*}
		else
		{
			SendErrorMessageToClient(new Message(msgType.DELETEDIRECTORY, inputMessage.filePath));
			return null;
		}*/



	}
	/**
	 * 
	 * @param filepath
	 * @param filename
	 * @param index
	 * @param opID
	 */
	public void CreateFile(Message message, int opID){
		String filepath = message.filePath;
		String filename = message.fileName;
		int index = message.chunkindex;
		String newfilename = filepath + "\\" + filename;
		String hashstring = newfilename + message.chunkindex;
		System.out.println("CREATING FILE " + newfilename);
		// if folder doesn't exist or file already exists
		if (NamespaceMap.get(filepath) == null
				|| chunkServerMap.get(hashstring) != null
				|| NamespaceMap.get(filepath).type == nodeType.FILE) {
			SendErrorMessageToClient(new Message(msgType.CREATEFILE, filename));
		} else {
			if(AddExclusiveParentLocks(filepath, opID))
			{
				String newName = filepath + "\\" + filename;
				if (NamespaceMap.get(newfilename) == null) {
					NamespaceMap.put(newName, new NamespaceNode(nodeType.FILE));

					NamespaceMap.get(filepath).children.add(newName);

					ChunkMetadata newChunk = new ChunkMetadata(newName, index, 1, 0);

					Random rand = new Random();
					newChunk.filenumber = rand.nextInt(5); //only use one for now
					newChunk.chunkHash = hashstring;
					chunkServerMap.put(hashstring, newChunk);

					message.type = msgType.CREATEFILE;
					message.chunkClass = newChunk;
					try {
						SendMessageToClient(message);

					} catch (Exception e) {
						//TODO: deal with message failure
						//newMessage.success = msgSuccess.REQUESTERROR;
						//SendMessageToClient(new Message(msgType.CREATEFILE, filename));
					}
				}

				WritePersistentNamespaceMap(newName, NamespaceMap.get(newName));
				WritePersistentChunkServerMap(hashstring,
						chunkServerMap.get(hashstring));
				SendSuccessMessageToClient(new Message(msgType.CREATEFILE, filename));
				tfsLogger.LogMsg("Created file " + newName);

			} else {

				SendErrorMessageToClient(new Message(msgType.CREATEFILE, filename));
				/*
				 * ServerSocket serverSocket; try { serverSocket = new
				 * ServerSocket(myPortNumber); Socket clientSocket =
				 * serverSocket.accept(); ObjectInputStream input = new
				 * ObjectInputStream( clientSocket.getInputStream()); File file = new
				 * File(message.filePath); Message responseMsg; if (!file.exists()) {
				 * file.mkdir(); // TODO: insert into map // TODO: assign chunk and
				 * replicas to chunk servers ChunkMetadata chunkData = new
				 * ChunkMetadata(); // TODO: set chunkData data responseMsg = new
				 * Message(msgType.CREATEDIRECTORY, chunkData); responseMsg.success =
				 * msgSuccess.SUCCESS; ObjectOutputStream out = new ObjectOutputStream(
				 * clientSocket.getOutputStream()); out.writeObject(responseMsg); //
				 * TODO: message chunk servers } else { responseMsg = new
				 * Message(msgType.CREATEDIRECTORY); responseMsg.success =
				 * msgSuccess.ERROR; ObjectOutputStream out = new ObjectOutputStream(
				 * clientSocket.getOutputStream()); out.writeObject(responseMsg); } }
				 * catch (IOException e) { // TODO Auto-generated catch block
				 * e.printStackTrace(); }
				 */
			}
		}
	}
	/**
	 * 
	 * @param filepath
	 * @param opID
	 */
	public void CreateDirectory(String filepath, int opID) {
		if (!NamespaceMap.containsKey(filepath)) { // directory doesn't exist
			if(AddExclusiveParentLocks(filepath, opID))
			{
				File path = new File(filepath);
				String parentPath = path.getParent();
				String parent;
				if (parentPath == null) {
					parent = filepath;
				} else {
					parent = parentPath;
				}
				if (!NamespaceMap.containsKey(parent) && !(parent.equals(filepath))) {
					// parent directory does not exist
					SendErrorMessageToClient(new Message(msgType.CREATEDIRECTORY, filepath));
					return;
				} else if (NamespaceMap.containsKey(parent)) {
					NamespaceMap.get(parent).children.add(filepath);
				}

				NamespaceNode newNode = new NamespaceNode(nodeType.DIRECTORY);
				NamespaceMap.put(filepath, newNode);
				SendSuccessMessageToClient(new Message(msgType.CREATEDIRECTORY,
						filepath));
				tfsLogger.LogMsg("Created directory " + filepath);

				WritePersistentNamespaceMap(filepath, newNode);
				System.out.println("output " + opID);
			}
			else
			{
				SendErrorMessageToClient(new Message(msgType.CREATEDIRECTORY, filepath));
			}

		} else // directory already exists
		{
			SendErrorMessageToClient(new Message(msgType.CREATEDIRECTORY, filepath));
		}
	}

	/**
	 * 
	 * @param message
	 * @param opID
	 */
	public void AppendToTFSFile(Message message, int opID)
	{
		if(AddExclusiveParentLocks(message.filePath, opID))
		{
			ChunkMetadata chunkData = GetTFSFile(message.filePath);
			if(chunkData != null) {
				Message m1 = new Message(msgType.APPENDTOTFSFILE, chunkData);
				Message m2 = new Message(msgType.APPENDTOTFSFILE, chunkData);
				SendMessageToChunkServer(m1);
				SendMessageToClient(m2);
			}
			else {
				SendErrorMessageToClient(new Message(msgType.CREATEFILE, message.filePath));
			}
		}
		else
		{
			SendErrorMessageToClient(new Message(msgType.CREATEFILE, message.filePath));
		}
	}

	/**
	 * @param filepath
	 * @return
	 */
	public ChunkMetadata GetTFSFile(String filepath)
	{
		int index = 1;
		String hashString = filepath + index;
		if(NamespaceMap.containsKey(filepath)) // return existing ChunkMetadata
		{
			for(int i = 1; i <= chunkServerMap.size(); i++) {
				index++;
				hashString = filepath + index;
				if(!chunkServerMap.containsKey(hashString)) {
					break;
				}
			}
			System.out.println("INDEX: "+index);
			System.out.println("HASHSTRING: "+hashString);
			ChunkMetadata newChunk = new ChunkMetadata(filepath, index, 1, 0);
			newChunk.filenumber = 0; //only use one for now
			newChunk.chunkHash = hashString;
			chunkServerMap.put(hashString, newChunk);
			WritePersistentChunkServerMap(hashString,
					chunkServerMap.get(hashString));
			return newChunk;
		}
		else
		{
			// create file
			NamespaceMap.put(filepath, new NamespaceNode(nodeType.FILE));
			File filePath = new File(filepath);
			String parentPath = filePath.getParent();
			String parent;
			if (parentPath == null) {
				System.out.println("Can not find parent path");
				return null;
			} else {
				parent = parentPath;
			}
			NamespaceMap.get(parent).children.add(filepath);
			ChunkMetadata newChunk = new ChunkMetadata(filepath, 1, 1, 0);
			//Random rand = new Random();
			newChunk.filenumber = 1; //only use one for now
			newChunk.chunkHash = hashString;
			chunkServerMap.put(hashString, newChunk);

			WritePersistentNamespaceMap(filepath, NamespaceMap.get(filepath));
			WritePersistentChunkServerMap(hashString,
					chunkServerMap.get(hashString));
			tfsLogger.LogMsg("Created file " + filepath);
			return newChunk;
		}
	}
	/**
	 * 
	 * @param filepath
	 * @param opID
	 */
	public void FindFile(String filepath, int opID)
	{
		if(AddSharedParentLocks(filepath, opID))
		{
			int index = 1;
			int logicalFilesCount = 0;
			String chunkServerMapKey = filepath + index;
			if(NamespaceMap.containsKey(filepath))
			{
				ChunkMetadata chunkDataFinding;// = NamespaceMap.get(filepath);

				while(chunkServerMap.containsKey(chunkServerMapKey)){
					logicalFilesCount++;
					chunkDataFinding = chunkServerMap.get(chunkServerMapKey);
					Message newMessage = new Message(msgType.COUNTFILES, chunkDataFinding);
					newMessage.chunkClass.filename = filepath;
					//					try {
					//						chunkServer.DealWithMessage(newMessage);
					//
					//					} catch (Exception e) {
					//						SendErrorMessageToClient(new Message(msgType.COUNTFILES, filepath));
					//					}
					index++;
					chunkServerMapKey = filepath + index;
				}
				if(logicalFilesCount ==1)
					System.out.println("There is " + logicalFilesCount + " logical file in " + filepath);
				else
					System.out.println("There are " + logicalFilesCount + " logical files in " + filepath);
			}
			else
			{
				System.out.println("File does not exist...");
			}
		}
	}

	// ///////////////////////////WRITING TO PERSISTENT DATA///////////////////////////

	/**
	 * @param key
	 * @param chunkmd
	 */
	public void WritePersistentChunkServerMap(String key, ChunkMetadata chunkmd) {
		// String fileToWriteTo = "dataStorage/File" + chunkmd.filenumber;

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
		BufferedWriter out = null;
		File file = null;
		FileWriter ofstream = null;
		try {
			file = new File("dataStorage/MData_ChunkServerMap.txt");
			ofstream = new FileWriter(file.getAbsoluteFile(), true); // true
			// tells
			// to
			// append
			// data.
			out = new BufferedWriter(ofstream);
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
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			try {
				out.close();
				ofstream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param key
	 * @param nsNode
	 */
	public void WritePersistentNamespaceMap(String key, NamespaceNode nsNode) {
		// STRUCTURE///
		// KEY TYPE CHILD CHILD CHILD ...//
		BufferedWriter out = null;
		try {
			File file = new File("dataStorage/MData_NamespaceMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), true); // true
			// tells
			// to
			// append
			// data.
			out = new BufferedWriter(fstream);
			// System.out.println("Writing out to file");
			out.write(key + "\t" + nsNode.type.toString() + "\t");
			if (nsNode.children.size() > 0) {
				for (int i = 0; i < nsNode.children.size(); i++) {
					out.write(nsNode.children.get(i) + "\t");
				}
			}

			out.newLine();
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
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
	public void LoadChunkServerMap() {
		BufferedReader textReader = null;
		try {
			File file = new File("dataStorage/MData_ChunkServerMap.txt");
			FileReader fr = new FileReader(file);
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

				chunkServerMap.put(key, newMetaData);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
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
	public void LoadNamespaceMap() {
		// String path = "dataStorage/MData_NamespaceMap.txt";
		BufferedReader textReader = null;
		try {
			File file = new File("dataStorage/MData_NamespaceMap.txt");
			FileReader fr = new FileReader(file);
			textReader = new BufferedReader(fr);

			String textLine;

			while ((textLine = textReader.readLine()) != null) {
				// STRUCTURE///
				// KEY CHILD CHILD CHILD ...//
				String[] data = textLine.split("\t");
				String key;
				List<String> children = new ArrayList<String>();
				key = data[0];
				String stringEnum = data[1];
				nodeType type;
				if (stringEnum.equals("DIRECTORY")) {
					type = nodeType.DIRECTORY;
				} else {
					type = nodeType.FILE;
				}
				for (int i = 2; i < data.length; i++) {
					children.add(data[i]);
				}

				// TODO fix
				NamespaceNode addingNode = new NamespaceNode(nodeType.DIRECTORY);
				addingNode.children = children;
				addingNode.type = type;

				NamespaceMap.put(key, addingNode);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
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
	public void ClearChunkServerMapFile() {
		BufferedWriter out = null;
		try {
			File file = new File("dataStorage/MData_ChunkServerMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false); // true
			// tells
			// to
			// append
			// data.
			out = new BufferedWriter(fstream);
			// System.out.println("Writing out to file");
			out.write("");
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
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
	public void ClearNamespaceMapFile() {
		BufferedWriter out = null;
		try {
			File file = new File("dataStorage/MData_NamespaceMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false); // true
			// tells
			// to
			// append
			// data.
			out = new BufferedWriter(fstream);
			// System.out.println("Writing out to file");
			out.write("");
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void LoadServerData() {
		for (int i = 2; i <= 5; i++) {
			String IP = Config.prop.getProperty("IP" + i);
			int clientPort = Integer.parseInt(Config.prop.getProperty("PORT"
					+ i + "_CLIENT"));
			int serverPort = Integer.parseInt(Config.prop.getProperty("PORT"
					+ i + "_SERVER"));
			ServerData temp = new ServerData(IP, clientPort, serverPort);
			ServerMap.put(IP, temp);
			System.out.println("Server at IP " + IP + " added to network");
			System.out.println("ClientPort: " + clientPort + "\t ServerPort: "
					+ serverPort);
		}
	}

	/////////////////////////////END OF PERSISTENT DATA FUNCTIONS//////////////////////////////
	////////////////////////////START OF HEARTBEAT FUNCTIONS///////////////////////////////////
	/**
	 * 
	 * @param HBMessage
	 */
	public void SetChunkServerDead(HeartBeat HBMessage)
	{
		//TODO: Is the map key the IP?
		String IPOfDownChunkServer = HBMessage.receiverIP;

		if(ServerMap.containsKey(IPOfDownChunkServer))
		{
			ServerMap.get(IPOfDownChunkServer).status = serverStatus.DEAD;
		}
	}

	public void SetChunkServerOutdated(String IPaddress)
	{
		if(ServerMap.containsKey(IPaddress))
		{
			ServerMap.get(IPaddress).status = serverStatus.OUTDATED;
		}

		for(Map.Entry<String, ChunkMetadata> cmEntry : chunkServerMap.entrySet())
		{
			for(ChunkLocation location: cmEntry.getValue().listOfLocations)
			{
				if(location.chunkIP == IPaddress) //&& cmEntry.getValue().listOfLocations.size() > 1)
				{
					//Send message with the chunkMetaData to the chunkserver
					//from there, the chunkserver can determine if it has the correct version

				}
			}
		}

	}

	public void SetChunkServerAlive(String IPaddress)
	{
		if(ServerMap.containsKey(IPaddress))
		{
			ServerMap.get(IPaddress).status = serverStatus.ALIVE;
		}
	}
}
