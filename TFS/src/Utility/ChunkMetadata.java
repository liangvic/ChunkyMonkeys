package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(int version,List<chunkLocation> chList,int hash, int count){
		versionNumber = version;
		listOfLocations = chList;
		chunkHash = hash;
		referenceCount = count;
	}
	public ChunkMetadata(String filename, int chunkindex, int versionnumber, int refCount){
		versionNumber = versionnumber;
		index = chunkindex;
		chunkHash = ((String)(filename + index)).hashCode();
		referenceCount = refCount;
	}
	public String filename;
	public int filenumber;
	public int byteoffset;
	public int index;
	public int size;	
	int versionNumber;
	
	List<chunkLocation> listOfLocations = new ArrayList<chunkLocation>();
	public int chunkHash;
	int referenceCount;

	
	
}