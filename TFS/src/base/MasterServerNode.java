package base;

import java.io.*;
import java.net.*;
import java.util.*;

import Utility.ChunkMetadata;
import Utility.Message;
import Utility.TFSLogger;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.NamespaceNode;
import Utility.chunkLocation;

public class MasterServerNode extends ServerNode {
	public ClientServerNode client;
	public ChunkServerNode chunkServer;

	// private static ServerSocket welcomeSocket;

	Map<String,ChunkMetadata> chunkServerMap = new HashMap<String,ChunkMetadata>();
	Map<String, NamespaceNode> NamespaceMap = new HashMap<String, NamespaceNode>();
	TFSLogger tfsLogger = new TFSLogger();
	
	public MasterServerNode()
	{
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
				SendSuccessMessageToClient();
			} else {
				SendErrorMessageToClient();
			}
		}
		else if(inputMessage.type == msgType.CREATEDIRECTORY && inputMessage.sender == serverType.CLIENT) 
		{
			CreateDirectory(inputMessage.filePath);
		}
		else if (inputMessage.type == msgType.CREATEFILE)
		{			
			if (inputMessage.sender == serverType.CLIENT)
				CreateFile(inputMessage.filePath, inputMessage.fileName,  inputMessage.chunkClass.index);
			else if (inputMessage.sender == serverType.CHUNKSERVER){
				if (inputMessage.success == msgSuccess.REQUESTSUCCESS)
					System.out.println("File " + inputMessage.chunkClass.filename + " creation successful");
				else if (inputMessage.success == msgSuccess.REQUESTERROR)
					System.out.println("File " + inputMessage.chunkClass.filename + " creation failed");
			}
		}
		
	}

	public void SendSuccessMessageToClient() {
		Message successMessage = new Message(msgType.CREATEDIRECTORY);
		successMessage.success = msgSuccess.REQUESTSUCCESS;
		client.DealWithMessage(successMessage);
	}

	public void SendErrorMessageToClient() {
		Message successMessage = new Message(msgType.CREATEDIRECTORY);
		successMessage.success = msgSuccess.REQUESTERROR;
		client.DealWithMessage(successMessage);
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
			// finally delete directory wanted to delete
			NamespaceMap.remove(filePath);
			
		} 
		else // the filepath is not in the directory. Send error!
		{
			System.out
					.println("Error! That filepath is not in the directory! Aborting deletion...");
			Message errorMessageToClient = new Message(msgType.DELETEDIRECTORY);
			errorMessageToClient.success = msgSuccess.REQUESTSUCCESS;
			client.DealWithMessage(errorMessageToClient);
			return;
		}
	}

	public void deleteAllChildNodes(String startingNodeFilePath) {
		if (NamespaceMap.get(startingNodeFilePath).children.size() == 0) {
			NamespaceMap.remove(startingNodeFilePath);
			
			int chunkIndex = 1;
			String hashPath = startingNodeFilePath+chunkIndex;
			while(chunkServerMap.containsKey(hashPath.hashCode()))
			{
				// Send message to client server to erase data
				Message clientMessage = new Message(msgType.DELETEDIRECTORY);
				
				clientMessage.chunkClass = chunkServerMap.get(hashPath.hashCode()); // does NS tree
													
				// sending protocol
				chunkServer.DealWithMessage(clientMessage);
				chunkIndex++;
				hashPath = startingNodeFilePath + chunkIndex;
			}
			return;
		} 
		else {
			for (int i = 0; i < NamespaceMap.get(startingNodeFilePath).children.size(); i++) {
				deleteAllChildNodes(NamespaceMap.get(startingNodeFilePath).children.get(i));
			}
		}
	}

	public void CreateFile(String filepath, String filename, int index){
		String hashstring = filepath + "\\" + filename + index;
		int hash = hashstring.hashCode();
		//if folder doesn't exist or file already exists
		if (NamespaceMap.get(filepath) == null || chunkServerMap.get(hash) != null){
			SendErrorMessageToClient();
		}
		else
		{
			String newName = filepath + "\\" + filename;
			NamespaceMap.get(filepath).children.add(newName);
			NamespaceMap.put(newName, new NamespaceNode());
			ChunkMetadata newChunk = new ChunkMetadata(newName, index, 1, 1); 
			
	    	Random rand = new Random();	
			newChunk.filenumber = rand.nextInt(5);
			chunkServerMap.put(newName, newChunk);			
			
			Message newMessage = new Message(msgType.CREATEFILE,newChunk);
			chunkServer.DealWithMessage(newMessage);
		}
	}

	public void CreateDirectory(String filepath)
	{
		if (!NamespaceMap.containsKey(filepath)) { // directory doesn't exist
			NamespaceNode newNode = new NamespaceNode();
			NamespaceMap.put(filepath, newNode);
			File file = new File(filepath);
			file.mkdirs();
			SendSuccessMessageToClient();
			tfsLogger.LogMsg("Created directory "+filepath);
		}
		else // directory already exists
		{
			SendErrorMessageToClient();
		}

			/*ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(myPortNumber);
			Socket clientSocket = serverSocket.accept();
			ObjectInputStream input = new ObjectInputStream(
					clientSocket.getInputStream());
			File file = new File(message.filePath);
			Message responseMsg;
			if (!file.exists()) {
				file.mkdir();
				// TODO: insert into map
				// TODO: assign chunk and replicas to chunk servers
				ChunkMetadata chunkData = new ChunkMetadata();
				// TODO: set chunkData data
				responseMsg = new Message(msgType.CREATEDIRECTORY, chunkData);
				responseMsg.success = msgSuccess.SUCCESS;
				ObjectOutputStream out = new ObjectOutputStream(
						clientSocket.getOutputStream());
				out.writeObject(responseMsg);
				// TODO: message chunk servers
			} else {
				responseMsg = new Message(msgType.CREATEDIRECTORY);
				responseMsg.success = msgSuccess.ERROR;
				ObjectOutputStream out = new ObjectOutputStream(
						clientSocket.getOutputStream());
				out.writeObject(responseMsg);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	public void WritePersistentChunkServerMap(String key, ChunkMetadata chunkmd)
	{
		String fileToWriteTo = "dataStorage/File" + chunkmd.filenumber;
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
		    FileWriter fstream = new FileWriter(fileToWriteTo, true); //true tells to append data.
		    out = new BufferedWriter(fstream);
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
	}
	
	public void WritePersistentNamespaceMap(String key,NamespaceNode nsNode)
	{
		String fileToWriteTo = "dataStorage/MData_NamespaceMap.txt";
		//STRUCTURE///
		//KEY CHILD CHILD CHILD ...//
		BufferedWriter out = null;
		try  
		{
		    FileWriter fstream = new FileWriter(fileToWriteTo, true); //true tells to append data.
		    out = new BufferedWriter(fstream);
		    out.write(key+"\t");
		    for(int i=0;i<nsNode.children.size();i++)
		    {
		    	out.write(nsNode.children.get(i)+ "\t");
		    }
		    out.newLine();
		}
		catch (IOException e)
		{
		    System.err.println("Error: " + e.getMessage());
		}
	}
	
	public void LoadChunkServerMap()
	{
		String path = "dataStorage/MData_ChunkServerMap.txt";
		try {
			FileReader fr = new FileReader(path);
			BufferedReader textReader = new BufferedReader(fr);
			String textLine;
			
			while((textLine = textReader.readLine())!= null)
			{
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
				String[] data = textLine.split("\t");
				
				//key
				String key;
				key = data[0];
				
				//version
				int n_version = Integer.parseInt(data[1]);
				
				//location
				List<chunkLocation> locations = new ArrayList<chunkLocation>();
				int locationSize = locations.size();
				int newIndexCounter = 3 + (locationSize/2);
				for(int i=3; i<newIndexCounter; i=i+2)
				{
					locations.add(new chunkLocation(data[i],Integer.parseInt(data[i+1])));
				}
				
				//hash
				List<Integer> hash = new ArrayList<Integer>();
				String n_tempHash = data[newIndexCounter++];
				for(int i=0;i<n_tempHash.length();i++)
				{
					hash.add(Character.getNumericValue(n_tempHash.charAt(i)));//adds at end
				}
				n_tempHash = hash.toString();
				
				//count
				int n_count = Integer.parseInt(data[newIndexCounter++]);
				
				//filename
				String n_fileName = data[newIndexCounter++];
				
				//fileNumber
				int n_fileNumber = Integer.parseInt(data[newIndexCounter++]);
				
				//byteoffset
				int n_byteOffset = Integer.parseInt(data[newIndexCounter++]);
				
				//index
				int n_index = Integer.parseInt(data[newIndexCounter++]);
				
				//size
				int n_size = Integer.parseInt(data[newIndexCounter++]);

				ChunkMetadata newMetaData = new ChunkMetadata(n_fileName,n_index,n_version,n_count);
				newMetaData.listOfLocations = locations;
				newMetaData.chunkHash = Integer.parseInt(n_tempHash);
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
	
	public void LoadNamespaceMap()
	{
		String path = "dataStorage/MData_NamespaceMap.txt";
		try {
			FileReader fr = new FileReader(path);
			BufferedReader textReader = new BufferedReader(fr);
			
			String textLine;
			
			while((textLine = textReader.readLine())!= null)
			{
				//STRUCTURE///
				//KEY CHILD CHILD CHILD ...//
				String[] data = textLine.split("\t");
				String key;
				List<String> children = new ArrayList<String>();
				key = data[0];
				for(int i= 1; i< data.length;i++)
				{
					children.add(data[i]);
				}
				
				NamespaceNode addingNode = new NamespaceNode();
				addingNode.children = children;
				
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
}
