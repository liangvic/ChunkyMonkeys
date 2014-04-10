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
	
//	List<Character> filePath = new ArrayList<Character>();
	public static enum msgType {CREATEDIRECTORY,DELETEDIRECTORY};
	public msgType type;
	public String filePath;
	int startByte;
	int byteLength;
	ChunkMetadata chunkClass;
}
