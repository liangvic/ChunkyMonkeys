package Utility;

import java.net.*;  
import java.util.*;
import java.io.*; 

public class Message implements Serializable{
	
	public Message(){
		
	}
	//This is for test5
	public Message(String fp){
		filePath = fp;
	}
//	List<Character> filePath = new ArrayList<Character>();
	String filePath;
	int startByte;
	int byteLength;
	ChunkMetadata chunkClass;
	
}
