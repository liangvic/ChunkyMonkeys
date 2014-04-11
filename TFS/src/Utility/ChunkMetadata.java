package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(){
		
	}
	public ChunkMetadata(String filename, int ind, int v, int refCount){
		versionNumber = v;
		index = ind;
		chunkHash = ((String)(filename + index)).hashCode();
		referenceCount = refCount;
	}
	String filename;
	public int filenumber;
	public int byteoffset;
	public int index;
	public int size;	
	int versionNumber;

	List<ChunkLocation> listOfLocations = new ArrayList<ChunkLocation>();
	public int chunkHash;

	int referenceCount;

	
	
}