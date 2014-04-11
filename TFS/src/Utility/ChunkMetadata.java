package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(int version,List<chunkLocation> chList,int hash, int count){
		versionNumber = version;
		listOfLocations = chList;
		chunkHash = hash;
		referenceCount = count;
	}
	public ChunkMetadata(String filename, int ind, int v, int refCount){
		versionNumber = v;
		index = ind;
		chunkHash = ((String)(filename + index)).hashCode();
		referenceCount = refCount;
	}
	String filename;
	public int filenumber;
	int byteoffset;
	public int index;
	int size;	
	int versionNumber;
	
	List<chunkLocation> listOfLocations = new ArrayList<chunkLocation>();
	public int chunkHash;
	int referenceCount;

	
	
}