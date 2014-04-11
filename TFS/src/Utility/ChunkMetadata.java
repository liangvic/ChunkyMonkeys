package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(String filename, int chunkindex, int versionnumber, int refCount){
		versionNumber = versionnumber;
		index = chunkindex;
		chunkHash = (filename + Integer.toString(index));
		referenceCount = refCount;
	}
	public int versionNumber;
	public List<ChunkLocation> listOfLocations = new ArrayList<ChunkLocation>();
	public String chunkHash;
	public int referenceCount;
	public String filename;
	public int filenumber;
	public int byteoffset;
	public int index;
	public int size;	



	
	
}