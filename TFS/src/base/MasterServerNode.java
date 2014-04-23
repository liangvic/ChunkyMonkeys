package base;

import java.io.*;
import java.net.*;
import java.util.*;

import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Message;
import Utility.NamespaceNode.nodeType;
import Utility.TFSLogger;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.NamespaceNode;

public class MasterServerNode extends ServerNode {
	public ClientServerNode client;
	public ChunkServerNode chunkServer;

	// private static ServerSocket welcomeSocket;
	//chunkServerMap key is the filepath + chunk index
	Map<String, ChunkMetadata> chunkServerMap = new HashMap<String, ChunkMetadata>();
	Map<String, NamespaceNode> NamespaceMap = new HashMap<String, NamespaceNode>();
	TFSLogger tfsLogger = new TFSLogger();

	public MasterServerNode() {
		LoadChunkServerMap();
		LoadNamespaceMap();
	}

	// Don't call on this for now; using monolith structure
	public void WILLBEMAIN() throws Exception {
		int portNumber = 8111;

		try (ServerSocket serverSocket = new ServerSocket(portNumber);
				Socket clientSocket = serverSocket.accept();
				/*
				 * PrintWriter out = new
				 * PrintWriter(clientSocket.getOutputStream(), true);
				 * BufferedReader in = new BufferedReader( new
				 * InputStreamReader(clientSocket.getInputStream()));
				 */
				ObjectInputStream in = new ObjectInputStream(
						clientSocket.getInputStream());) {
			/*
			 * String inputLine; while ((inputLine = in.readLine()) != null) {
			 * //out.println(inputLine); DealWithMessage(inputLine); //separate
			 * message to deal with input }
			 */

			Message receivedMessage = (Message) in.readObject();
			DealWithMessage(receivedMessage);
		} catch (IOException e) {
			System.out
					.println("Exception caught when trying to listen on port "
							+ portNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
		/*
		 * String clientSentence; String capitalizedSentence; Properties prop =
		 * new Properties(); prop.load(new
		 * FileInputStream("config/config.properties"));
		 * System.out.println(prop.getProperty("IP1")); welcomeSocket = new
		 * ServerSocket(9090);
		 * 
		 * while(true) { Socket connectionSocket = welcomeSocket.accept();
		 * BufferedReader inFromClient = new BufferedReader(new
		 * InputStreamReader(connectionSocket.getInputStream()));
		 * DataOutputStream outToClient = new
		 * DataOutputStream(connectionSocket.getOutputStream()); clientSentence
		 * = inFromClient.readLine(); System.out.println("Received: " +
		 * clientSentence); capitalizedSentence = clientSentence.toUpperCase() +
		 * '\n'; outToClient.writeBytes(capitalizedSentence); }
		 */
	}

	public void DealWithMessage(Message inputMessage) {
		System.out.println("inputMessagetype "+ inputMessage.type);
		if (inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CLIENT) {
			MDeleteDirectory(inputMessage.filePath);
		} else if (inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CHUNKSERVER) {
			if (inputMessage.success == msgSuccess.REQUESTSUCCESS) {
				// SendSuccessMessageToClient();
			} else {
				// SendErrorMessageToClient();
			}
		} else if (inputMessage.type == msgType.CREATEDIRECTORY) {
			if (inputMessage.sender == serverType.CLIENT) {
				try {
					CreateDirectory(inputMessage.filePath);
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
		} else if (inputMessage.type == msgType.CREATEFILE) {
			if (inputMessage.sender == serverType.CLIENT)
				CreateFile(inputMessage.filePath, inputMessage.fileName,
						inputMessage.chunkindex);
			else if (inputMessage.sender == serverType.CHUNKSERVER) {
				if (inputMessage.success == msgSuccess.REQUESTSUCCESS)
					System.out.println("File "
							+ inputMessage.chunkClass.filename
							+ " creation successful");
				else if (inputMessage.success == msgSuccess.REQUESTERROR)
					System.out.println("File "
							+ inputMessage.chunkClass.filename
							+ " creation failed");
			}
		} else if (inputMessage.type == msgType.READFILE && inputMessage.sender == serverType.CLIENT) {
			ReadFile(inputMessage);
		}
		else if(inputMessage.type == msgType.APPENDTOFILE)
		{
			if(inputMessage.sender == serverType.CLIENT)
				AssignChunkServer(inputMessage);
			else if (inputMessage.sender == serverType.CHUNKSERVER){
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
				AppendToTFSFile(inputMessage);
			}
			else if(inputMessage.sender == serverType.CHUNKSERVER) {
				if(inputMessage.success == msgSuccess.REQUESTSUCCESS){
					System.out.println("File "+ inputMessage.chunkClass.filename + " append successful");
				}
				else if (inputMessage.success == msgSuccess.REQUESTERROR)
					System.out.println("File " + inputMessage.chunkClass.filename + " append failed");
			}
		}
		else if(inputMessage.type == msgType.COUNTFILES)
		{
			if(inputMessage.sender == serverType.CLIENT)
			{
				FindFile(inputMessage.filePath);
			}
			else if (inputMessage.sender == serverType.CHUNKSERVER)
			{
				System.out.println("There are " + inputMessage.countedLogicalFiles + " logical files in " + inputMessage.filePath);
			}
		}

	}

	public void SendSuccessMessageToClient(Message successMessage) {
		successMessage.success = msgSuccess.REQUESTSUCCESS;
		client.DealWithMessage(successMessage);
	}

	public void SendErrorMessageToClient(Message errorMessage) {
		errorMessage.success = msgSuccess.REQUESTERROR;
		client.DealWithMessage(errorMessage);
	}

	public void MDeleteDirectory(String filePath) {

		if (NamespaceMap.containsKey(filePath)) {
			// now that have the node in the NamespaceTree, you iterate through
			// it's children
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
			
		} else // the filepath is not in the directory. Send error!
		{
			SendErrorMessageToClient(new Message(msgType.DELETEDIRECTORY, filePath));
			return;
		}
	}

	public void deleteAllChildNodes(String startingNodeFilePath) {
		if (NamespaceMap.get(startingNodeFilePath).children.size() == 0) {
			//initially start at chunk index 1
			int chunkIndex = 1;
			String chunkServerKey = startingNodeFilePath + chunkIndex;
			
			// Send message to client server to erase data IF IS FILE
			if(NamespaceMap.get(startingNodeFilePath).type == nodeType.FILE)
			{
				while(chunkServerMap.containsKey(chunkServerKey))
				{
					//System.out.println("Going to delete the value");
					// sending protocol
					Message clientMessage = new Message(msgType.DELETEDIRECTORY);
					clientMessage.chunkClass = chunkServerMap.get(chunkServerKey);
					chunkServer.DealWithMessage(clientMessage);
					
					//delete the file from master's chunk server map
					chunkServerMap.remove(chunkServerKey);
					
					//increment for checking if there are more chunks
					chunkIndex++;
					chunkServerKey = startingNodeFilePath + chunkIndex;
				}
			}
			
			//remove the FILE or DIRECTORY from namespace map
			NamespaceMap.remove(startingNodeFilePath);
			//tfsLogger.LogMsg("Created file " + startingNodeFilePath);
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

	public void ReadFile(Message inputMessage) {
		//Implement Later
		int indexCounter = 1;
		System.out.println("trying to read "+inputMessage.filePath + indexCounter);
		if (chunkServerMap.containsKey(inputMessage.filePath + indexCounter)) {
			// master extracts the chunkclass from the filepath key
			ChunkMetadata cm = chunkServerMap.get(inputMessage.filePath+ indexCounter);
			System.out.println("hoho lets see here "+cm.chunkHash);
			Message returnMessage = new Message(msgType.READFILE, cm);
			returnMessage.success = msgSuccess.REQUESTSUCCESS;
			client.DealWithMessage(returnMessage);
		}
		else{
			System.out.println("doesnt exist");
			SendErrorMessageToClient(new Message(msgType.READFILE, inputMessage.filePath));
			return;
		}
		// check if the file contains multiple chunk indexes
		indexCounter++;
		while (chunkServerMap.containsKey(inputMessage.filePath + indexCounter)) {
			ChunkMetadata cm = chunkServerMap.get(inputMessage.filePath
					+ indexCounter);
			Message returnMessage = new Message(msgType.READFILE, cm);
			returnMessage.success = msgSuccess.REQUESTSUCCESS;
			client.DealWithMessage(returnMessage);
			indexCounter++;
		}
		System.out.println("M: There is no file index "+indexCounter);
//		client.ExpectChunkNumberForRead(indexCounter - 1);
	}

	public ChunkMetadata AssignChunkServer(Message inputMessage){
		String hashstring = inputMessage.filePath + "\\" + inputMessage.fileName + 1;
		//System.out.println("burrito: "+inputMessage.fileName);
		ChunkMetadata newMetaData = new ChunkMetadata(inputMessage.fileName, 1,1,0);
		//newMetaData.listOfLocations = 0;
		newMetaData.chunkHash = hashstring;
		Random rand = new Random();
		newMetaData.filenumber = rand.nextInt(5);
		//do a check to see what the offset is
		int targetFileNumber = newMetaData.filenumber;
		int largestOffSet = 0;
		
		for(String key: chunkServerMap.keySet()){
			if(chunkServerMap.get(key).filenumber == targetFileNumber)
				if(chunkServerMap.get(key).byteoffset>largestOffSet)
					largestOffSet = chunkServerMap.get(key).byteoffset;
		}
		newMetaData.byteoffset = largestOffSet;
		newMetaData.size = inputMessage.fileData.length;
		
		
		//add to hashmap
		chunkServerMap.put(hashstring, newMetaData);
		//create a new namespace node
		//filename and get parent, add child.
		
		
		
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
		
		Message metadataMsg = new Message(msgType.APPENDTOFILE, newMetaData);
		client.DealWithMessage(metadataMsg);
		return newMetaData;
		//client.AppendToChunkServer(hashstring, myServer);
		//client.AppendToChunkServer(newMetaData, chunkServer);
		
		
	}
	
	public void CreateFile(String filepath, String filename, int index){
		System.out.println("CREATING FILE");
		String newfilename = filepath + "\\" + filename;
		String hashstring = newfilename + index;
		// if folder doesn't exist or file already exists
		if (NamespaceMap.get(filepath) == null
				|| chunkServerMap.get(hashstring) != null
				|| NamespaceMap.get(filepath).type == nodeType.FILE) {
			SendErrorMessageToClient(new Message(msgType.CREATEFILE, filename));
		} else {

			String newName = filepath + "\\" + filename;
			if (NamespaceMap.get(newfilename) == null) {
				NamespaceMap.put(newName, new NamespaceNode(nodeType.FILE));

				NamespaceMap.get(filepath).children.add(newName);

				ChunkMetadata newChunk = new ChunkMetadata(newName, index, 1, 0);

				Random rand = new Random();
				newChunk.filenumber = 0; //only use one for now
				newChunk.chunkHash = hashstring;
				chunkServerMap.put(hashstring, newChunk);

				Message newMessage = new Message(msgType.CREATEFILE, newChunk);
				newMessage.chunkClass.filename = newName;
				try {
					chunkServer.DealWithMessage(newMessage);

				} catch (Exception e) {
					SendErrorMessageToClient(new Message(msgType.CREATEFILE, filename));
				}

				WritePersistentNamespaceMap(newName, NamespaceMap.get(newName));
				WritePersistentChunkServerMap(hashstring,
						chunkServerMap.get(hashstring));
				SendSuccessMessageToClient(new Message(msgType.CREATEFILE, filename));
				tfsLogger.LogMsg("Created file " + newName);
				
			} else {
				SendErrorMessageToClient(new Message(msgType.CREATEFILE, filename));
			}

		}
	}

	public void CreateDirectory(String filepath) {
		if (!NamespaceMap.containsKey(filepath)) { // directory doesn't exist
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
		} else // directory already exists
		{
			SendErrorMessageToClient(new Message(msgType.CREATEDIRECTORY, filepath));
		}

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
	
	public void AppendToTFSFile(Message message)
	{
		ChunkMetadata chunkData = GetTFSFile(message.filePath);
		if(chunkData != null) {
			Message m1 = new Message(msgType.APPENDTOTFSFILE, chunkData);
			Message m2 = new Message(msgType.APPENDTOTFSFILE, chunkData);
			chunkServer.DealWithMessage(m1);
			client.DealWithMessage(m2);
		}
		else {
			SendErrorMessageToClient(new Message(msgType.CREATEFILE, message.filePath));
		}
	}
	
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

	public void FindFile(String filepath)
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
//				try {
//					chunkServer.DealWithMessage(newMessage);
//
//				} catch (Exception e) {
//					SendErrorMessageToClient(new Message(msgType.COUNTFILES, filepath));
//				}
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
	
	/////////////////////////////WRITING TO PERSISTENT DATA///////////////////////////
	
	public void WritePersistentChunkServerMap(String key, ChunkMetadata chunkmd)
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
		FileWriter ofstream = null;
		try  
		{
			file = new File("dataStorage/MData_ChunkServerMap.txt");
		    ofstream = new FileWriter(file.getAbsoluteFile(), true); //true tells to append data.
		    out = new BufferedWriter(ofstream);
		    out.write(key+"\t"+chunkmd.versionNumber+"\t"+chunkmd.listOfLocations.size()+"\t");
		    for(int i=0;i<chunkmd.listOfLocations.size();i++)
		    {
		    	out.write(chunkmd.listOfLocations.get(i).chunkIP + "\t" + chunkmd.listOfLocations.get(i).chunkPort+ "\t");
		    }
		    out.write(chunkmd.chunkHash + "\t" +chunkmd.referenceCount + "\t" + chunkmd.filename + "\t");
		    out.write(chunkmd.filenumber + "\t" + chunkmd.byteoffset + "\t" + chunkmd.index + "\t" + chunkmd.size);
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
				ofstream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

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
	
	public void LoadChunkServerMap()
	{	
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

	public void ClearNamespaceMapFile() {
		BufferedWriter out = null;
		try  
		{
		   File file = new File("dataStorage/MData_NamespaceMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false); //true tells to append data.
		    out = new BufferedWriter(fstream);
		    //System.out.println("Writing out to file");
		    out.write("");
		}catch (IOException e) {
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

}
