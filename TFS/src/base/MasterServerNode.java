package base;

import java.io.*;
import java.net.*;
import java.util.*;

import Utility.ChunkMetadata;
import Utility.ChunkMetadata.chunkLocation;
import Utility.Message;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.NamespaceNode;

public class MasterServerNode extends ServerNode {
	public ClientServerNode client;
	public ChunkServerNode chunkServer;

	Map<String,ChunkMetadata> chunkServerMap = new HashMap<String,ChunkMetadata>();
	Map<String,NamespaceNode> NamespaceMap = new HashMap<String,NamespaceNode>();
	
	public MasterServerNode()
	{
		LoadChunkServerMap();
		LoadNamespaceMap();
	}
	
	public void LoadChunkServerMap()
	{
		String path = "/dataStorage/MData_ChunkServerMap.txt";
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
				String[] data = textLine.split("\t");
				String key;
				key = data[0];
				
				int version = Integer.parseInt(data[1]);
				
				List<chunkLocation> locations = new ArrayList<chunkLocation>();
				int locationSize = locations.size();
				
				List<Integer> hash = new ArrayList<Integer>();
				int count;
				
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
	
	public void LoadNamespaceMap()
	{
		String path = "/dataStorage/MData_NamespaceMap.txt";
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
			CreateDirectory();
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
			
			// Send message to client server to erase data
			Message clientMessage = new Message(msgType.DELETEDIRECTORY);
			clientMessage.chunkClass = chunkServerMap.get(startingNodeFilePath); // does NS tree
																// hold this?
			// sending protocol
			chunkServer.DealWithMessage(clientMessage);
			return;
		} 
		else {
			for (int i = 0; i < NamespaceMap.get(startingNodeFilePath).children.size(); i++) {
				deleteAllChildNodes(NamespaceMap.get(startingNodeFilePath).children.get(i));
			}
		}
	}
	public void CreateDirectory()
	{
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

}
