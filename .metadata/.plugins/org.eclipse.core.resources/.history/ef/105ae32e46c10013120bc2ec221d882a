package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(){
		
	}
	public ChunkMetadata(String filename, int v, int refCount){
		versionNumber = v;
		chunkHash = filename.hashCode();
		referenceCount = refCount;
	}
	String filename;
	public int filenumber;
	int byteoffset;
	int size;	
	int versionNumber;
	
	List<chunkLocation> listOfLocations = new ArrayList<chunkLocation>();
	public int chunkHash;
	int referenceCount;
	public class chunkLocation{
		String chunkIP;
		int chunkPort;
		
		public chunkLocation(String ip, int port){
			chunkIP = ip;
			chunkPort = port;
		}
	}
	
	
}