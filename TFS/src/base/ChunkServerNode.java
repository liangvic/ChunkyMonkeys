package base;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import Utility.ChunkLocation;
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
    	ChunkLocation location;
    	List<Character> chunkHash = new ArrayList<Character>();
    	List<Byte> data = new ArrayList<Byte>();
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
    	else if(message.type == msgType.READFILE)
    	{
    		ReadChunks(message.chunkClass);
    	}
	}
    public void ReadChunks(ChunkMetadata metadata){
    	List<List<Byte>> fileMetaData = new ArrayList<List<Byte>>();
    	for(ChunkLocation messageLocation: metadata.listOfLocations){
    		for(listMetaData fileData: files){
    			if((fileData.location.chunkIP == messageLocation.chunkIP) && (fileData.location.chunkPort == messageLocation.chunkPort)){
    				fileMetaData.add(fileData.data);
    			}
    		}
    	}
    	
    	client.DealWithMessage(new Message(msgType.PRINTFILEDATA, fileMetaData));
    	
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
    		successMessageToMaster.success = msgSuccess.SUCCESS;
    		master.DealWithMessage(successMessageToMaster);
    	}
    }
}
