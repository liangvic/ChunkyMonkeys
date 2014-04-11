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
	int versionNumber;

	public List<ChunkLocation> listOfLocations = new ArrayList<ChunkLocation>();
	public List<Character> chunkHash = new ArrayList<Character>();
	int referenceCount;

	
	
}