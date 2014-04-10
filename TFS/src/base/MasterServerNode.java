package base;
import java.io.*;
import java.net.*;
import java.util.*;

import Utility.ChunkMetadata;
import Utility.Message;
import Utility.Message.msgType;
import Utility.NamespaceNode;

public class MasterServerNode extends ServerNode{
	
	//private static ServerSocket welcomeSocket;

	Map<String,ChunkMetadata> chunkServerMap = new HashMap<String,ChunkMetadata>();
	static LinkedList<NamespaceNode> NamespaceTree = new LinkedList<NamespaceNode>();
	
	public static void main(String args[]) throws Exception
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
	
	public static void DealWithMessage(Message inputMessage)
	{
		if(inputMessage.type == msgType.DELETEDIRECTORY)
		{
			MDeleteDirectory(inputMessage.filePath);
		}
	}
	
	public static void MDeleteDirectory(String filePath)
	{
		if(NamespaceTree.contains(filePath))
		{
			String[] tokens = filePath.split(File.pathSeparator);
			NamespaceNode nextNode = (NamespaceNode)NamespaceTree.get(0);
			
			if(tokens[0] == nextNode.filename)
			{
				for(int i = 0; i<nextNode.children.size();i++)
				{
					for (String directoryName : tokens)
					{
						if(directoryName == nextNode.filename)
						{
							
						}
					}
				}
			}
		}
		else //the filepath is not in the directory
		{
			
		}
	}
	
	
}
