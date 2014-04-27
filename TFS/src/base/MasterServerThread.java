package base;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

import base.MasterServerNode.ServerData;
import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Config;
import Utility.HeartBeat;
import Utility.Message;
import Utility.NamespaceNode;
import Utility.SOSMessage;
import Utility.SOSMessage.msgTypeToServer;
import Utility.TFSLogger;
import Utility.lockInfo;
import Utility.HeartBeat.serverStatus;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.NamespaceNode.lockType;
import Utility.NamespaceNode.nodeType;
import Utility.SOSMessage.msgTypeToMaster;

public class MasterServerThread extends ServerThread {
	MasterServerNode server;
	Map<String, NamespaceNode> NamespaceMap;
	Semaphore lockChange;
	Semaphore fileWriteSemaphore;
	Map<String, ChunkMetadata> chunkServerMap;
	Map<String, ServerData> ServerMap;
	TFSLogger tfsLogger;
	String myIP;

	public MasterServerThread(MasterServerNode sn, Socket s) {
		super(sn, s);
		server = sn;
		NamespaceMap = server.NamespaceMap;
		lockChange = server.lockChange;
		fileWriteSemaphore = server.fileWriteSemaphore;
		chunkServerMap = server.chunkServerMap;
		ServerMap = server.ServerMap;
		tfsLogger = server.tfsLogger;
		myIP = server.myIP;
	}

	public void DealWithMessage(Message inputMessage) {
		server.operationID++; //used to differentiate operations
		System.out.println("inputMessagetype "+ inputMessage.type);
		if(inputMessage instanceof HeartBeat)
		{
			
		}
		else if(inputMessage instanceof SOSMessage)
		{
			if(((SOSMessage)inputMessage).msgToMaster == msgTypeToMaster.REQUESTINGDATA)
			{
				TellOtherChunkServerToSendData((SOSMessage)inputMessage);
			}
			else if(((SOSMessage)inputMessage).msgToMaster == msgTypeToMaster.DONESENDING)
			{
				//TODO: finished with the sending of data -- release semaphore-kind of thing?
			}
		}
		else if (inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CLIENT) {
			MDeleteDirectory(inputMessage, server.operationID);
		} else if (inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CHUNKSERVER) {
			RemoveParentLocks(inputMessage.filePath,inputMessage.opID);
			if (inputMessage.success == msgSuccess.REQUESTSUCCESS) {
				// SendSuccessMessageToClient();
			} else {
				// SendErrorMessageToClient();
			}

		} else if (inputMessage.type == msgType.CREATEDIRECTORY) {
			if (inputMessage.sender == serverType.CLIENT) {
				try {
					CreateDirectory(inputMessage, server.operationID);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (inputMessage.sender == serverType.CHUNKSERVER) {
				RemoveParentLocks(inputMessage.filePath,inputMessage.opID);
				if (inputMessage.success == msgSuccess.REQUESTSUCCESS) {
					SendSuccessMessageToClient(inputMessage);
				} else {
					SendErrorMessageToClient(inputMessage);
				}

			}
		} else if (inputMessage.type == msgType.READFILE) {
			if(inputMessage.sender == serverType.CLIENT)
			{
				ReadFile(inputMessage, server.operationID);
			}
			else if (inputMessage.sender == serverType.CHUNKSERVER)
			{
				RemoveParentLocks(inputMessage.filePath, inputMessage.opID);
				//TODO: NEED TO ADD IN FURTHER IF STATEMENTS
			}
		}

		else if (inputMessage.type == msgType.CREATEFILE) {
			if (inputMessage.sender == serverType.CLIENT)
				CreateFile(inputMessage, server.operationID);
			else if (inputMessage.sender == serverType.CHUNKSERVER) {
				RemoveParentLocks(inputMessage.filePath,inputMessage.opID);
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
				ReadFile(inputMessage, server.operationID);
			}
			else if (inputMessage.sender == serverType.CHUNKSERVER)
			{
				RemoveParentLocks(inputMessage.filePath, inputMessage.opID);
				//TODO: NEED TO ADD IN FURTHER IF STATEMENTS
			}
		}
		else if(inputMessage.type == msgType.APPENDTOFILE)
		{
			if(inputMessage.sender == serverType.CLIENT)
				AssignChunkServer(inputMessage, server.operationID);//, operationID);
			else if (inputMessage.sender == serverType.CHUNKSERVER){
				RemoveParentLocks(inputMessage.filePath, inputMessage.opID);
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
				//should retrieve ip and port for chunkserver who has the filepath here
				AppendToTFSFile(inputMessage, server.operationID);
			}
			else if(inputMessage.sender == serverType.CHUNKSERVER) {
				RemoveParentLocks(inputMessage.filePath, inputMessage.opID);
				if(inputMessage.success == msgSuccess.REQUESTSUCCESS){
					System.out.println("File "+ inputMessage.chunkClass.filename + " append successful");
				}
			}
		}else if(inputMessage.type == msgType.WRITETONEWFILE) // Test 4 & Unit 4
		{
			AssignChunkServer(inputMessage, server.operationID);
		}	
		/*
				else if (inputMessage.type == msgType.APPENDTOTFSFILE) // Test 6
				{

					FindFile(inputMessage.filePath, operationID);
				}
				else if (inputMessage.sender == serverType.CHUNKSERVER)
				{
					RemoveParentLocks(inputMessage.filePath);
					System.out.println("There are " + inputMessage.countedLogicalFiles + " logical files in " + inputMessage.filePath);
				}*/

		server.messageList.remove(inputMessage);

	}


	/**
	 * 
	 * @param opID
	 */
	public void RemoveParentLocks(String filePath, int opID)
	{
		for(Map.Entry<String, NamespaceNode> entry : NamespaceMap.entrySet())
		{
			//if this operation previously made the lock
			for(lockInfo lock: entry.getValue().lockList)
			{
				if(lock.operationID == opID)
				{
					//lock.lockStatus = lockType.NONE;
					entry.getValue().lockList.remove(lock);
				}
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
		/*{
			lockChange.acquire();
		} catch (InterruptedException e) {
			System.out.println("Couldn't acquire semaphore");
			e.printStackTrace();
			return false;
		}*/
		String[] tokens = filePath.split(File.pathSeparator);
		String parentPath = tokens[0];
		for(int i=1;i<tokens.length-1;i++)
		{
			if(NamespaceMap.containsKey(filePath))
			{
				for(lockInfo nsNode: NamespaceMap.get(parentPath).lockList)
				{
					if(nsNode.operationID == opID)
					{
						if(nsNode.lockStatus == lockType.NONE)
						{
							if(parentPath != filePath)
							{
								NamespaceMap.get(filePath).lockList.add(new lockInfo(lockType.I_EXCLUSIVE,opID));
								//nsNode.lockStatus = lockType.I_EXCLUSIVE;
								//nsNode.operationID = opID;
							}
							else
							{
								NamespaceMap.get(filePath).lockList.add(new lockInfo(lockType.EXCLUSIVE,opID));
								//NamespaceMap.get(parentPath).lockList.remove(opID);
								//NamespaceMap.get(parentPath).lockData.lockStatus = lockType.EXCLUSIVE;
								//NamespaceMap.get(parentPath).lockData.operationID = opID;
							}
						}
						else if(nsNode.lockStatus == lockType.I_EXCLUSIVE ||
								nsNode.lockStatus == lockType.I_SHARED)
						{
							if(parentPath == filePath)
							{
								RemoveParentLocks(filePath, opID);
								return false;
							}
							//if not the final node, allow it to pass
						}
						else if(nsNode.lockStatus == lockType.SHARED ||
								nsNode.lockStatus == lockType.EXCLUSIVE)
						{
							RemoveParentLocks(parentPath, opID);
							return false;
						}
					}
				}

				parentPath = parentPath + "\\" + tokens[i]; 
			}
		}
		//lockChange.release();
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
		try{
			lockChange.acquire();
		}
		catch (InterruptedException e){
			System.out.println("Couldn't acquire semaphore");
			e.printStackTrace();
			return false;
		}
		String[] tokens = filePath.split(File.pathSeparator);
		String parentPath = tokens[0];
		for(int i=1;i<tokens.length-1;i++)
		{
			if(NamespaceMap.containsKey(filePath))
			{
				for(lockInfo nsNode: NamespaceMap.get(parentPath).lockList)
				{
					if(nsNode.operationID == opID)
					{
						if(nsNode.lockStatus == lockType.NONE)
						{
							if(parentPath != filePath)
							{
								NamespaceMap.get(filePath).lockList.add(new lockInfo(lockType.I_SHARED,opID));
							}
							else
							{
								NamespaceMap.get(filePath).lockList.add(new lockInfo(lockType.SHARED,opID));
							}
						}
						else if(nsNode.lockStatus == lockType.I_EXCLUSIVE ||
								nsNode.lockStatus == lockType.I_SHARED)
						{
							if(parentPath == filePath)
							{
								RemoveParentLocks(filePath,opID);
								return false;
							}
							//if not the final node, allow it to pass
						}
						else if(nsNode.lockStatus == lockType.EXCLUSIVE)
						{
							RemoveParentLocks(parentPath,opID);
							return false;
						}
					}
				}

				parentPath = parentPath + "\\" + tokens[i]; 
			}
		}
		lockChange.release();
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
		//TODO: CHeck message integrity
		server.SendMessage(message);
	}

	/** 
	 * @param clientServerMessage
	 */
	public void SendMessageToClient(Message message) {
		server.SendMessage(message);
	}

	/**
	 * 
	 * @param msg
	 * @param opID
	 */
	public void MDeleteDirectory(Message msg, int opID) {
		String filePath = msg.filePath;
		if (NamespaceMap.containsKey(filePath)) {
			// now that have the node in the NamespaceTree, you iterate through
			// it's children
			if(AddExclusiveParentLocks(filePath, opID))
			{
				if (NamespaceMap.get(filePath).children.size() > 0) {
					// recursively going through the tree and deleting all
					// files/directories below
					deleteAllChildNodes(filePath, msg);
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
				SendSuccessMessageToClient(msg);
			}
			else
			{
				SendErrorMessageToClient(msg);
				return;
			}

		}
	}

	/**
	 * @param startingNodeFilePath
	 */
	public void deleteAllChildNodes(String startingNodeFilePath, Message msg) {
		if (NamespaceMap.get(startingNodeFilePath).children.size() == 0) {
			// initially start at chunk index 1
			int chunkIndex = 1;
			String chunkServerKey = startingNodeFilePath + chunkIndex;

			// Send message to client server to erase data IF IS FILE
			if (NamespaceMap.get(startingNodeFilePath).type == nodeType.FILE) {
				while (chunkServerMap.containsKey(chunkServerKey)) {
					// System.out.println("Going to delete the value");
					// sending protocol
					//TODO: send delete message to respective server
					//		ChunkMetadata metadata = chunkServerMap.get(chunkServerKey);
					//		String rip = metadata.listOfLocations.
					//		Message chunkMessage = new Message(myIP, myType, myPortNumber, rip, 
					//		chunkMessage.chunkClass = chunkServerMap
					//				.get(chunkServerKey);
					//AAA		SendMessageToChunkServer(chunkMessage);

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
						.get(i),msg);
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
			if (!chunkServerMap.containsKey(inputMessage.filePath + indexCounter) && !NamespaceMap.containsKey(inputMessage.filePath + indexCounter)) {
				System.out.println("Master: doesnt exist");
				SendErrorMessageToClient(inputMessage);
				return;
			}
			else if (!chunkServerMap.containsKey(inputMessage.filePath + indexCounter) && NamespaceMap.containsKey(inputMessage.filePath + indexCounter))
			{
				//if the file exists in the chunkservermap and not in the namespacemap, it means that hte file is empty
				System.out.println("The file you desired is empty");
				SendSuccessMessageToClient(inputMessage);
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

			Message expectMsg = inputMessage;
			expectMsg.type = msgType.EXPECTEDNUMCHUNKREAD;

			expectMsg.success = msgSuccess.REQUESTSUCCESS;
			expectMsg.expectNumChunkForRead = indexCounter-1;
			SendMessageToClient(expectMsg);
			for(int i=1;i<indexCounter;i++){
				ChunkMetadata cm = chunkServerMap.get(inputMessage.filePath+ i);
				System.out.println("Master: first chunkhash is "+cm.chunkHash);
				Message returnMessage = inputMessage;
				returnMessage.type = msgType.READFILE;
				returnMessage.chunkClass = cm;
				returnMessage.success = msgSuccess.REQUESTSUCCESS;
				SendMessageToClient(returnMessage);
			}
		}
		else
		{
			SendErrorMessageToClient(inputMessage);
		}
	}

	/**
	 * 
	 * @param inputMessage
	 * @return
	 */
	public void AssignChunkServer(Message inputMessage, int opID){//assign multiple chunk servers
		//TODO: NEED TO ADD IN THE LOCK CHECKING
		if(AddExclusiveParentLocks(inputMessage.filePath, opID))
		{
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


			//Assigns a file number from 0 - 4



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
			//Make the location list
			List<ChunkLocation> newLocations = new ArrayList<ChunkLocation>();
			for(int j=0;j<replicaList.size();j++){
				ChunkLocation location = new ChunkLocation(replicaList.get(j).IP,replicaList.get(j).serverPort);
				location.fileNumber = targetFileNumber;
				location.byteOffset = replicaListLargestOffset[j];
				//add to position j
				newLocations.add(location);
			}
			
			ChunkMetadata newMetaData = new ChunkMetadata(inputMessage.fileName, 1,1,0);
			newMetaData.chunkHash = hashstring;
			newMetaData.filenumber = targetFileNumber;
			newMetaData.listOfLocations = newLocations;
//			newMetaData.byteoffset = replicaListLargestOffset[i];
			newMetaData.size = inputMessage.fileData.length;
			
			chunkServerMap.put(hashstring, newMetaData);
			inputMessage.chunkClass = newMetaData;
			inputMessage.addressedTo = serverType.CLIENT;
			inputMessage.sender = serverType.MASTER;
			SendMessageToClient(inputMessage);
			
			//Sending a create file for each replica
			for(int i = 0;i<replicaList.size();i++){

				
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

			//client.AppendToChunkServer(hashstring, myServer);
			//client.AppendToChunkServer(newMetaData, chunkServer);
			/*}
		else
		{
			SendErrorMessageToClient(new Message(msgType.DELETEDIRECTORY, inputMessage.filePath));
			return null;
		}*/
		}
		else
		{
			System.out.println("AssignChunkServerFunction failed");
		}
	}
	/**
	 * 
	 * @param filepath
	 * @param filename
	 * @param index
	 * @param opID
	 */
	public void CreateFile(Message message, int opID){
		//NOT ADDING IT TO THE CHUNKSERVER MAP. WHEN APPENDING LATER, THEN ADDING TO CHUNKSERVERMAP
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
			SendErrorMessageToClient(message);
		} else {
			if(AddExclusiveParentLocks(filepath, opID))
			{
				String newName = filepath + "\\" + filename;
				if (NamespaceMap.get(newfilename) == null) {
					NamespaceMap.put(newName, new NamespaceNode(nodeType.FILE));

					NamespaceMap.get(filepath).children.add(newName);

					/*ChunkMetadata newChunk = new ChunkMetadata(newName, index, 1, 0);

					Random rand = new Random();
					newChunk.filenumber = rand.nextInt(5); //only use one for now
					newChunk.chunkHash = hashstring;*/
					/*
					//randomly selecting which 3 chunkservers to put it in
					int numReplicas = 0, randomNum;
					ServerData attemptServerData;
					boolean validIP = false;
					while(numReplicas < 1) //TODO: FIX boundary
					{
						validIP = true;
						randomNum = rand.nextInt(1); //TODO: Change back to 4
						attemptServerData = ServerMap.get(randomNum);
						for(ChunkLocation location : newChunk.listOfLocations) //check if already given to that IP
						{
							if(attemptServerData.IP.equals(location.chunkIP) && attemptServerData.serverPort == location.chunkPort) //if already added chunk to that chunkserver already, then don't use it again
							{
								validIP = false;
							}
							
						}		
						if(validIP)
						{
							newChunk.listOfLocations.add(new ChunkLocation(attemptServerData.IP,attemptServerData.clientPort));
							numReplicas++;
						}
					}
					*/
					//chunkServerMap.put(hashstring, newChunk);

					message.type = msgType.CREATEFILE;
					//message.sender = serverType.MASTER;
					//message.senderIP = myIP;
					//message.chunkClass = newChunk;
					try {
						SendMessageToClient(message);
						/*for(ChunkLocation entry : message.chunkClass.listOfLocations)
						{
							message.receiverIP = entry.chunkIP;
							message.receiverInputPort = entry.chunkPort;
							SendMessageToChunkServer(message);
						}*/
	
					} catch (Exception e) {
						//TODO: deal with message failure
						//newMessage.success = msgSuccess.REQUESTERROR;
						//SendMessageToClient(new Message(msgType.CREATEFILE, filename));
					}
				}

				//ClearNamespaceMapFile();
				WritePersistentNamespaceMap(newName, NamespaceMap.get(newName));
				//WritePersistentChunkServerMap(hashstring,
				//		chunkServerMap.get(hashstring));
				SendSuccessMessageToClient(message);
				tfsLogger.LogMsg("Created file " + newName);

			} else {

				SendErrorMessageToClient(message);
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
	public void CreateDirectory(Message message, int opID) {
		String filepath = message.filePath;
		System.out.println("Trying to create file path: "+ message.filePath);
		if (!NamespaceMap.containsKey(filepath)) { // directory doesn't exist
			if(true)//AddExclusiveParentLocks(filepath, opID))
			{
				File path = new File(filepath);
				String parentPath = path.getParent();
				String parent;
				if (parentPath == null) {
					parent = filepath;
				} else {
					parent = parentPath;
				}
				for(int r=0;r<10;r++){
					if (!NamespaceMap.containsKey(parent) && !(parent.equals(filepath))) {
						// parent directory does not exist
						System.out.println("Parent directory doesnt exist");
						if(r<9){
							continue;
						}
						SendErrorMessageToClient(message);
						return;
					} else if (NamespaceMap.containsKey(parent)) {
						NamespaceMap.get(parent).children.add(filepath);
						break;
					}
				}

				NamespaceNode newNode = new NamespaceNode(nodeType.DIRECTORY);
				NamespaceMap.put(filepath, newNode);
				SendSuccessMessageToClient(message);
				tfsLogger.LogMsg("Created directory " + filepath);

				ClearNamespaceMapFile();
				for(Map.Entry<String, NamespaceNode> cmEntry : NamespaceMap.entrySet())
				{
					WritePersistentNamespaceMap(cmEntry.getKey(), cmEntry.getValue());
				}
				System.out.println("OPID " + opID + " Finished");
			}
			/*else
			{
				SendErrorMessageToClient(message);
			}*/

		} else // directory already exists
		{
			SendErrorMessageToClient(message);
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
				message.chunkClass = chunkData;
				//TODO: FIX THIS
				//SendMessageToChunkServer(message);
				SendMessageToClient(message); //sends chunkClass with list of chunk locations to client
			}
			else {
				SendErrorMessageToClient(message);
			}
		}
		else
		{
			SendErrorMessageToClient(message);
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
				e.printStackTrace();
			}
		}
	}
	/**
	 * @param chunkmd
	 */
	public void WritePersistentChunkServerMap(String key, ChunkMetadata chunkmd) {
		// String fileToWriteTo = "dataStorage/File" + chunkmd.filenumber;

		// STRUCTURE///
		// KEY VERSION# SIZEOF_LOCATIONLIST
		// CHUNKLOCATION1_IP CHUNKLOCATION1_PORT 
		// CHUNKLOCATION1_BYTEOFFSET CHUNKLOCATION1_FILENUMBER
		//... CHUNKLOCATIONN_IP CHUNKLOCATIONN_PORT
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
						+ chunkmd.listOfLocations.get(i).chunkPort + "\t"
						+ chunkmd.listOfLocations.get(i).byteOffset + "\t"
						+ chunkmd.listOfLocations.get(i).fileNumber + "\t");
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
		// KEY TYPE CHILDLIST_SIZE CHILD CHILD CHILD 
		// LOCKLIST_SIZE LOCKSTATUS1 LOCKOPID1 ...
		// LOCKSTATUSN LOCKOPIDN 
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
				out.write(nsNode.children.size() + "\t");
				for (int i = 0; i < nsNode.children.size(); i++) {
					out.write(nsNode.children.get(i) + "\t");
				}
			}
			if (nsNode.lockList.size() > 0)
			{
				out.write(nsNode.lockList.size() + "\t");
				for (int i = 0; i < nsNode.lockList.size(); i++)
				{
					out.write(nsNode.lockList.get(i).lockStatus + "\t"
							+ nsNode.lockList.get(i).operationID + "\t");
				}
			}

			out.newLine();
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}



	/////////////////////////////END OF PERSISTENT DATA FUNCTIONS//////////////////////////////
	////////////////////////////START OF HEARTBEAT FUNCTIONS///////////////////////////////////
	public void TellOtherChunkServerToSendData(SOSMessage msg)
	{
		for(Map.Entry<String, ChunkMetadata> cmEntry : chunkServerMap.entrySet())
		{
			for(ChunkLocation location: cmEntry.getValue().listOfLocations)
			{
				if(location.chunkIP != msg.senderIP)
				{
					msg.receiverIP = location.chunkIP;
					msg.msgToMaster = msgTypeToMaster.DONESENDING;
					SendMessageToChunkServer(msg);
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
	
	public void SetChunkServerOutdated(String IPaddress)
	{
		if(ServerMap.containsKey(IPaddress))
		{
			ServerMap.get(IPaddress).status = serverStatus.OUTDATED;
		}
		SOSMessage message = new SOSMessage(server.myIP, server.myType, server.myInputPortNumber, IPaddress, serverType.CHUNKSERVER, ServerMap.get(IPaddress).clientPort);
		message.msgToServer = msgTypeToServer.TO_SOSSERVER;
		//message.chunkClass = ServerMap.get(IPaddress).
		SendMessageToChunkServer(message);
	}

}

