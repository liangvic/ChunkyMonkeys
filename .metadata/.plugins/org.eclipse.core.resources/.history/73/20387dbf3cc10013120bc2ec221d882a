package base;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import Utility.ChunkMetadata;
import Utility.Message;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;

public class ChunkServerNode extends ServerNode{
	public ClientServerNode client;
	public MasterServerNode master;
	
	String clientSentence;
    String capitalizedSentence;
	
    public class listMetaData{
    	int checksum; //??
    	int chunkVersion;
    	String location;
    	List<Character> chunkHash = new ArrayList<Character>();
    	List<Character> data = new ArrayList<Character>();
    }
    
    //TODO: check if we are using a list as data structure
    //Map<ChunkMetadata,char[]> chunkMap = new HashMap<ChunkMetadata,char[]>();
    List<listMetaData> files = new ArrayList<listMetaData>();
    
    
    public static void main(String argv[]) throws Exception
    {
       
       /*ServerSocket welcomeSocket = new ServerSocket(6789);

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
    
    public void DealWithMessage(Message message)
	{
    	if(message.type == msgType.DELETEDIRECTORY)
    	{
    		DeleteChunk(message.chunkClass);
    	}
	}
    
    public void DeleteChunk(ChunkMetadata metadata)
    {
    	listMetaData chunkToDelete = null;
    	boolean foundChunk = false;
    	for(listMetaData lmd: files)
    	{
    		if(lmd.chunkHash == metadata.chunkHash)
    		{
    			chunkToDelete = lmd;
    			foundChunk = true;
    		}
    	}
    	if(foundChunk)
    	{
    		files.remove(chunkToDelete);
    		Message successMessageToMaster = new Message(msgType.DELETEDIRECTORY);
    		successMessageToMaster.success = msgSuccess.REQUESTSUCCESS;
    		master.DealWithMessage(successMessageToMaster);
    	}
    }
}
