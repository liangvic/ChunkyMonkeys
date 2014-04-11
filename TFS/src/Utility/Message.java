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
	
//	List<Character> filePath = new ArrayList<Character>();
	public static enum msgType {CREATEDIRECTORY,DELETEDIRECTORY, CREATEFILE,READFILE,DELETEFROMSERVER};
	public msgType type;
	public String filePath;
	int startByte;
	int byteLength;
	public ChunkMetadata chunkClass;
}
