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
		if (inputMessage.type == msgType.DELETEDIRECTORY
				&& inputMessage.sender == serverType.CLIENT) {
			MDeleteDirectory(inputMessage.filePath);
		} else if (inputMessage.type == msgType.DELETEDIRECTORY
				&& inputMessage.sender == serverType.CHUNKSERVER) {
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
		} else if (inputMessage.type == msgType.READFILE
				&& inputMessage.sender == serverType.CLIENT) {
			ReadFile(inputMessage);
		}
		else if(inputMessage.type == msgType.APPENDTOFILE && inputMessage.sender == serverType.CLIENT)
		{
			if(inputMessage.sender == serverType.CLIENT)
				AssignChunkServer(inputMessage);
			else if (inputMessage.sender == serverType.CHUNKSERVER){
				if(inputMessage.success == msgSuccess.REQUESTSUCCESS)
					System.out.println("File "+ inputMessage.chunkClass.filename + " creation successful");
				else if (inputMessage.success == msgSuccess.REQUESTERROR)
					System.out.println("File " + inputMessage.chunkClass.filename + " creation failed");
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

			tfsLogger.LogMsg("Deleted directory in " + filePath);

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
			NamespaceMap.remove(startingNodeFilePath);

			int chunkIndex = 1;
			String hashPath = startingNodeFilePath + chunkIndex;
			if(chunkServerMap.containsKey(startingNodeFilePath))
			{
				chunkServerMap.remove(startingNodeFilePath);
			}
				// Send message to client server to erase data
				Message clientMessage = new Message(msgType.DELETEDIRECTORY);

				clientMessage.chunkClass = chunkServerMap.get(hashPath); // does
																			// NS
																			// tree
				chunkServerMap.remove(hashPath);
				
				// sending protocol
				//chunkServer.DealWithMessage(clientMessage);
				//chunkIndex++;
				//hashPath = startingNodeFilePath + chunkIndex;
				
				

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
		int indexCounter = 1;
		if (chunkServerMap.containsKey(inputMessage.fileName + indexCounter)) {
			// master extracts the chunkclass from the filepath key
			ChunkMetadata cm = chunkServerMap.get(inputMessage.fileName
					+ indexCounter);
			Message returnMessage = new Message(msgType.READFILE, cm);
			client.DealWithMessage(returnMessage);
		}
		else{
			SendErrorMessageToClient(new Message(msgType.READFILE, inputMessage.fileName));
			return;
		}
		// check if the file contains multiple chunk indexes
		indexCounter++;
		while (chunkServerMap.containsKey(inputMessage.fileName + indexCounter)) {
			ChunkMetadata cm = chunkServerMap.get(inputMessage.fileName
					+ indexCounter);
			Message returnMessage = new Message(msgType.READFILE, cm);
			client.DealWithMessage(returnMessage);
			indexCounter++;
		}
		client.ExpectChunkNumberForRead(indexCounter - 1);
	}

	public ChunkMetadata AssignChunkServer(Message inputMessage){
		String hashstring = inputMessage.filePath + "\\" + inputMessage.fileName + 1;
		ChunkMetadata newMetaData = new ChunkMetadata(inputMessage.fileName, 1,1,0);
		//newMetaData.listOfLocations = 0;
		newMetaData.chunkHash = hashstring;
		Random rand = new Random();
		newMetaData.filenumber = rand.nextInt(5);
		newMetaData.byteoffset = 0;
		newMetaData.size = inputMessage.fileData.length;
		
		//Message metadataMsg = new Message(msgType.APPENDTOFILE, newMetaData);
		return newMetaData;
		//client.AppendToChunkServer(hashstring, myServer);
		//client.AppendToChunkServer(newMetaData, chunkServer);
	}
	
	public void CreateFile(String filepath, String filename, int index){

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
				newChunk.filenumber = rand.nextInt(5);
				chunkServerMap.put(newName, newChunk);

				Message newMessage = new Message(msgType.CREATEFILE, newChunk);
				newMessage.chunkClass.filename = newName;
				try {
					chunkServer.DealWithMessage(newMessage);

				} catch (Exception e) {
					SendErrorMessageToClient(new Message(msgType.CREATEFILE, filename));
				}

				WritePersistentNamespaceMap(newName, NamespaceMap.get(newName));
				WritePersistentChunkServerMap(newName,
						chunkServerMap.get(newName));
				SendSuccessMessageToClient(new Message(msgType.CREATEFILE, filename));

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
		try  
		{
			File file = new File("dataStorage/MData_ChunkServerMap.txt");
		    FileWriter ofstream = new FileWriter(file.getAbsoluteFile(), true); //true tells to append data.
		    out = new BufferedWriter(ofstream);
		    out.write(key+"\t"+chunkmd.versionNumber+"\t"+chunkmd.listOfLocations.size()+"\t");
		    for(int i=0;i<chunkmd.listOfLocations.size();i++)
		    {
		    	out.write(chunkmd.listOfLocations.get(i).chunkIP + "\t" + chunkmd.listOfLocations.get(i).chunkPort+ "\t");
		    }
		    out.write(chunkmd.chunkHash + "\t" +chunkmd.referenceCount + "\t" + chunkmd.filename + "\t");
		    out.write(chunkmd.filenumber + "\t" + chunkmd.byteoffset + "\t" + chunkmd.index + "\t" + chunkmd.size);
		    out.newLine();
		    
		    out.close();
		    ofstream.close();
		}
		catch (IOException e)
		{
		    System.err.println("Error: " + e.getMessage());
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
			out.close();
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void LoadChunkServerMap()
	{	
		try {
			File file = new File("dataStorage/MData_ChunkServerMap.txt");
			FileReader fr = new FileReader(file);
			BufferedReader textReader = new BufferedReader(fr);
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
			textReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void LoadNamespaceMap() {
		// String path = "dataStorage/MData_NamespaceMap.txt";
		try {
			File file = new File("dataStorage/MData_NamespaceMap.txt");
			FileReader fr = new FileReader(file);
			BufferedReader textReader = new BufferedReader(fr);

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
			textReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
			System.out.println("Writing out to file");
			out.write("");
			out.close();
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void ClearNamespaceMapFile() {
		BufferedWriter out = null;
		try  
		{
		   File file = new File("dataStorage/MData_NamespaceMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false); //true tells to append data.
		    out = new BufferedWriter(fstream);
		    System.out.println("Writing out to file");
		    out.write("");
		    out.close();
		}catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

}
