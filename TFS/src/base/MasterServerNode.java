package base;
import java.io.*;
import java.net.*;
import java.util.*;

import Utility.ChunkMetadata;
import Utility.Message;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.NamespaceNode;

public class MasterServerNode extends ServerNode{
	public ClientServerNode client;
	public ChunkServerNode chunkServer;
	
	//private static ServerSocket welcomeSocket;

	Map<String,ChunkMetadata> chunkServerMap = new HashMap<String,ChunkMetadata>();
	static LinkedList<NamespaceNode> NamespaceTree = new LinkedList<NamespaceNode>();

	public void main(String args[]) throws Exception
    {
	        int portNumber = 8111;
	        
	        try (
	            ServerSocket serverSocket =
	                new ServerSocket(portNumber);
	            Socket clientSocket = serverSocket.accept();     
	            /*PrintWriter out =
	                new PrintWriter(clientSocket.getOutputStream(), true);                   
	            BufferedReader in = new BufferedReader(
	                new InputStreamReader(clientSocket.getInputStream()));*/
	        		ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
	        ) {
	           /* String inputLine;
	            while ((inputLine = in.readLine()) != null) {
	                //out.println(inputLine);
	            	DealWithMessage(inputLine); //separate message to deal with input
	            }*/
	        	
	        	Message receivedMessage = (Message) in.readObject();
	        	DealWithMessage(receivedMessage);
	        } catch (IOException e) {
	            System.out.println("Exception caught when trying to listen on port "
	                + portNumber + " or listening for a connection");
	            System.out.println(e.getMessage());
	        }
       /*String clientSentence;
       String capitalizedSentence;
       Properties prop = new Properties();
       prop.load(new FileInputStream("config/config.properties"));
       System.out.println(prop.getProperty("IP1"));
       welcomeSocket = new ServerSocket(9090);
       
       while(true)
       {
          Socket connectionSocket = welcomeSocket.accept();
          BufferedReader inFromClient =
             new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
          DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
          clientSentence = inFromClient.readLine();
          System.out.println("Received: " + clientSentence);
          capitalizedSentence = clientSentence.toUpperCase() + '\n';
          outToClient.writeBytes(capitalizedSentence);
       }*/
    }
	
	public void DealWithMessage(Message inputMessage)
	{
		if(inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CLIENT)
		{
			MDeleteDirectory(inputMessage.filePath);
		}
		else if(inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CHUNKSERVER)
		{
			if(inputMessage.success == msgSuccess.SUCCESS)
			{
				SendSuccessMessageToClient();
			}
			else
			{
				SendErrorMessageToClient();
			}
		}
	}
	public void SendSuccessMessageToClient()
	{
		Message successMessage = new Message(msgType.CREATEDIRECTORY);
		successMessage.success = msgSuccess.SUCCESS;
		client.DealWithMessage(successMessage);
	}
	
	public void SendErrorMessageToClient()
	{
		Message successMessage = new Message(msgType.CREATEDIRECTORY);
		successMessage.success = msgSuccess.ERROR;
		client.DealWithMessage(successMessage);
	}
	
	public void MDeleteDirectory(String filePath)
	{
		if(NamespaceTree.contains(filePath))
		{
			String[] tokens = filePath.split(File.pathSeparator);
			NamespaceNode currentNode = (NamespaceNode)NamespaceTree.get(0);
			
			//want to iterate through the NamespaceTree to make sure 
			//all directories in path exist
			String fullFilePath = null;
			//assuming that root directory exists already so start at 1
			for(int i=1;i<tokens.length;i++)
			{
				fullFilePath = "";
				//Need to concatenate the directories in token to a path
				for(int tokenIndex = 0; tokenIndex < i; tokenIndex++)
				{
					fullFilePath = fullFilePath + File.pathSeparator + tokens[tokenIndex];
				}
				
				//iterate through all children
				for(int j=0;j<currentNode.children.size();j++)
				{
					//if a child's filename = the next filename in path
					if(fullFilePath.equals(currentNode.children.get(j).filepath))
					{
						currentNode = currentNode.children.get(j);
						break;
					}
					//could not find the next directory in path name
					if(j==currentNode.children.size()-1)
					{
						//output an error message
						System.out.println("A directory in the path does not exist! No deletions done.");
						Message errorMessageToClient = new Message(msgType.DELETEDIRECTORY);
						errorMessageToClient.success = msgSuccess.SUCCESS;
						return;
					}
				}
			}
			//now that have the node in the NamespaceTree, you iterate through it's children
			if(currentNode.children.size() > 0)
			{
				//recursively going through the tree and deleting all files/directories below
				deleteAllChildNodes(NamespaceTree,currentNode);
			}
				
			//finally delete directory wanted to delete
			NamespaceTree.remove(currentNode);
		}
		else //the filepath is not in the directory. Send error!
		{
			System.out.println("Error! That filepath is not in the directory! Aborting deletion...");
			Message errorMessageToClient = new Message(msgType.DELETEDIRECTORY);
			errorMessageToClient.success = msgSuccess.SUCCESS;
			//need to send out
			
			return;
		}
		
	}
	
	public void deleteAllChildNodes(LinkedList<NamespaceNode> nsTree,NamespaceNode startingNode)
	{
		if(startingNode.children.size()==0)
		{
			nsTree.remove(startingNode);
			//Send message to client server to erase data
			Message clientMessage = new Message(msgType.DELETEDIRECTORY);
			clientMessage.chunkClass = startingNode.metaData; //does NS tree hold this?
			
			//sending protocol
			chunkServer.DealWithMessage(clientMessage);
			
			return;
		}
		else
		{
			for(int i=0;i<startingNode.children.size();i++)
			{
				deleteAllChildNodes(nsTree,startingNode.children.get(i));
			}
		}
	}
	public void CreateDirectory(Message message)
	{
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(myPortNumber);
			Socket clientSocket = serverSocket.accept();
			ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
			File file = new File(message.filePath);
			Message responseMsg;
			if(!file.exists()) {
				file.mkdir();
				//TODO: insert into map
				//TODO: assign chunk and replicas to chunk servers
				ChunkMetadata chunkData = new ChunkMetadata();
				//TODO: set chunkData data
				responseMsg = new Message(msgType.CREATEDIRECTORY, chunkData);
				responseMsg.success = msgSuccess.SUCCESS;
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				out.writeObject(responseMsg);
				//TODO: message chunk servers
			}
			else {
				responseMsg = new Message(msgType.CREATEDIRECTORY);
				responseMsg.success = msgSuccess.ERROR;
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				out.writeObject(responseMsg);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
