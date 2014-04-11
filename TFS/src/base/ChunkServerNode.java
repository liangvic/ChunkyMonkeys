package base;

import Utility.ChunkMetadata;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Utility.ChunkMetadata;
import Utility.Message;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;

public class ChunkServerNode extends ServerNode{
	public ClientServerNode client;
	public MasterServerNode master;
	
	public class File{
		int fileNumber;
		int spaceOccupied;
		byte[] data;
	}

	List<File> file_list = new ArrayList<File>();
			
	//hash to data
	Map<Integer, ChunkMetadata> chunkMap = new HashMap<Integer, ChunkMetadata>();	
    
    /* public static void main(String argv[]) throws Exception
    {
       
       ServerSocket welcomeSocket = new ServerSocket(6789);

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
       }
    }*/
    
    public void DealWithMessage(Message message)
	{
    	if(message.type == msgType.DELETEDIRECTORY)
    	{
    		DeleteChunk(message.chunkClass);
    	}
    	else if (message.type == msgType.CREATEFILE)
    	{
    		AddNewBlankChunk(message.chunkClass);
    	}
	}
    public void AddNewBlankChunk(ChunkMetadata metadata){
    	//TODO: have to create new Chunkmetadata and copy over metadata
    	chunkMap.put(metadata.chunkHash, metadata);
    	File current = file_list.get(metadata.filenumber);
    	metadata.byteoffset = current.spaceOccupied;
    	metadata.size = 0;
    	current.data[current.spaceOccupied] = 0;
    	current.data[current.spaceOccupied+1] = 0;
    	current.spaceOccupied+= 8;
    	
    	Message newMessage = new Message(msgType.CREATEDIRECTORY, metadata);
    	newMessage.success = msgSuccess.REQUESTSUCCESS;
    	master.DealWithMessage(newMessage);
    }
    
    public void DeleteChunk(ChunkMetadata metadata)
    {
    	/*listMetaData chunkToDelete = null;
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
    	}*/
    }
}
