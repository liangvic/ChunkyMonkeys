package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(){
		
	}
	int versionNumber;
	List<chunkLocation> listOfLocations = new ArrayList<chunkLocation>();
	public List<Integer> chunkHash = new ArrayList<Integer>();
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