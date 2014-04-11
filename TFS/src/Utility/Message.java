package Utility;

import java.net.*;  
import java.util.*;
import java.io.*; 

public class Message implements Serializable{
	
	public Message(msgType msgT){
		type = msgT;
	}
	//This is for test5
	public Message(String fp, msgType msgT){
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
	}
	public Message(msgType msgT, byte[] byteData) {
		type = msgT;
		fileData = byteData;
	}
//	List<Character> filePath = new ArrayList<Character>();
	public static enum msgType {CREATEDIRECTORY,DELETEDIRECTORY, CREATEFILE,READFILE,PRINTFILEDATA,UNKNOWNFILE};
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
	public ChunkMetadata chunkClass;
	public byte[] fileData;
}
