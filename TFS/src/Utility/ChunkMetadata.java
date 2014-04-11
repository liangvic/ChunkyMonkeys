package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(){
		
	}
	int versionNumber;
	public List<ChunkLocation> listOfLocations = new ArrayList<ChunkLocation>();
	public List<Character> chunkHash = new ArrayList<Character>();
	int referenceCount;
	
	
}