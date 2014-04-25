package Utility;

import java.net.*;  
import java.util.*;
import java.io.*; 

/**
 * Message class that gets passed between Nodes
 *
 */
public class Message implements Serializable{
		
	//This IS REQUIRED
	public Message(String senderip, serverType sendertype, int senderport, 
			String recieverip, serverType receivertype,int receiverport){
		addressedTo = receivertype;
		sender = sendertype;
		receiverIP = recieverip;
		senderIP = senderip;
		receiverInputPort = receiverport;
		senderInputPort = senderport;
	}
	
	public Message(msgType msgtype, String senderip, serverType sendertype, int senderport, 
		String recieverip, serverType receivertype,int receiverport){
		type = msgtype;
		addressedTo = receivertype;
		sender = sendertype;
		receiverIP = recieverip;
		senderIP = senderip;
		receiverInputPort = receiverport;
		senderInputPort = senderport;
	}
	/*
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
	}*/ 
	public static enum msgType {CREATEDIRECTORY,DELETEDIRECTORY, CREATEFILE,READFILE,PRINTFILEDATA,UNKNOWNFILE, APPENDTOFILE, APPENDTOTFSFILE, COUNTFILES, WRITETONEWFILE,EXPECTEDNUMCHUNKREAD};
	public msgType type;
	public static enum msgSuccess {REQUESTSUCCESS, REQUESTERROR};
	public msgSuccess success;
	public static enum serverType {MASTER,CLIENT,CHUNKSERVER};
	public serverType addressedTo;
	public serverType sender;
	public String filePath;
	public String fileName;
	public String localFilePath; //for use in AppendToTFSFile for now
	public int senderInputPort;
	public int receiverInputPort;
	int startByte;
	int byteLength;
	public int chunkindex;
	public ChunkMetadata chunkClass;
	public byte[] fileData;
	public int countedLogicalFiles;
	public int replicaCount;
	public String senderIP;
	public String receiverIP;
	public int expectNumChunkForRead;
	
}
