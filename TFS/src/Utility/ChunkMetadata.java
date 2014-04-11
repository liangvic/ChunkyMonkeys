package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(int version,List<chunkLocation> chList,List<Integer> hash, int count){
		versionNumber = version;
		listOfLocations = chList;
		chunkHash = hash;
		referenceCount = count;
	}
	int versionNumber;
	List<chunkLocation> listOfLocations = new ArrayList<chunkLocation>();
	public List<Integer> chunkHash = new ArrayList<Integer>();
	int referenceCount;
	
	
	
}