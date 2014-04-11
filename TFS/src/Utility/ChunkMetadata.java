package Utility;

import java.util.*;

public class ChunkMetadata{
	public ChunkMetadata(String filename, int chunkindex, int versionnumber, int refCount){
		versionNumber = versionnumber;
		index = chunkindex;
		chunkHash = ((String)(filename + index)).hashCode();
		referenceCount = refCount;
	}
	public int versionNumber;
	public List<chunkLocation> listOfLocations = new ArrayList<chunkLocation>();
	public int chunkHash;
	public int referenceCount;
	public String filename;
	public int filenumber;
	public int byteoffset;
	public int index;
	public int size;	


	
	
}