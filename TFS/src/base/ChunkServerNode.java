package base;

import Utility.ChunkMetadata;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Message;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;

public class ChunkServerNode extends ServerNode{
	public ClientServerNode client;
	public MasterServerNode master;


	public class File{
		int fileNumber;
		int spaceLeft;
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
		else if (message.type == msgType.READFILE)
		{
			ReadChunks(message.chunkClass);
		}
	}
	public void ReadChunks(ChunkMetadata metadata){
//		List<List<Byte>> fileMetaData = new ArrayList<List<Byte>>();
//		for(ChunkLocation messageLocation: metadata.listOfLocations){
//			for(File fileData: file_list){
//				if((fileData.location.chunkIP == messageLocation.chunkIP) && (fileData.location.chunkPort == messageLocation.chunkPort)){
//					fileMetaData.add(fileData.data);
//				}
//			}
//		}
//		
		for(File fileData:file_list){
			byte[] dataINeed = new byte[fileData.spaceLeft];
			if(metadata.filenumber == fileData.fileNumber){
				//check byte offset
				for(int i=0;i<fileData.spaceLeft;i++){
					dataINeed[i]=fileData.data[metadata.byteoffset+i];
				}
				client.DealWithMessage(new Message(msgType.PRINTFILEDATA ,dataINeed));
				break;
			}
		}

//		client.DealWithMessage(new Message(msgType.PRINTFILEDATA, fileMetaData));

	}
	public void AddNewBlankChunk(ChunkMetadata metadata){
		chunkMap.put(metadata.chunkHash, metadata);
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
