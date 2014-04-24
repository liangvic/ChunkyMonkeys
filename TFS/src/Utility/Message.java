package Utility;

import java.net.*;  
import java.util.*;
import java.io.*; 

/**
 * Message class that gets passed between Nodes
 *
 */
public class Message implements Serializable{
	
	public Message(msgType msgT){
		type = msgT;
	}
	//This is for test5
	public Message(String fp, msgType msgT){
		filePath = fp;
		type = msgT;
	}
	
	public Message(msgType msgT, String fp, int i){
		chunkindex = i;
		filePath = fp;
		type = msgT;
	}
	
	public Message(msgType msgT, String fp){
		filePath = fp;
		type = msgT;
	}
	public Message(msgType msgT, ChunkMetadata chunkData) {
		type = msgT;
		chunkClass = chunkData;
		chunkindex = chunkData.index;
		fileName = chunkData.filename;
	}
	public Message(msgType msgT, byte[] byteData) {
		type = msgT;
		fileData = byteData;
	}
	public Message() {
	}

	public static enum msgType {CREATEDIRECTORY,DELETEDIRECTORY, CREATEFILE,READFILE,PRINTFILEDATA,UNKNOWNFILE, APPENDTOFILE, APPENDTOTFSFILE, COUNTFILES, WRITETONEWFILE};
	public msgType type;
	public static enum msgSuccess {REQUESTSUCCESS, REQUESTERROR};
	public msgSuccess success;
	public static enum serverType {MASTER,CLIENT,CHUNKSERVER};
	public serverType addressedTo;
	public serverType sender;
	public String filePath;
	public String fileName;
	int startByte;
	int byteLength;
	public int chunkindex;
	public ChunkMetadata chunkClass;
	public byte[] fileData;
	public int countedLogicalFiles;
	public int replicas;
	public String senderIP;
	public String receiverIP;
}
