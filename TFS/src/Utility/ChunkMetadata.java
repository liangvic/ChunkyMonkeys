package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(){
		
	}
	int versionNumber;
	List<chunkLocation> listOfLocations = new ArrayList<chunkLocation>();
	public List<Character> chunkHash = new ArrayList<Character>();
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