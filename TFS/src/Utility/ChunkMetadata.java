package Utility;

import java.io.Serializable;
import java.util.*;

public class ChunkMetadata implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * @param fname
	 * @param chunkindex
	 * @param versionnumber
	 * @param refCount
	 */
	public ChunkMetadata(String fname, int chunkindex, int versionnumber, int refCount){
		versionNumber = versionnumber;
		index = chunkindex;
		chunkHash = (filename + Integer.toString(index));
		referenceCount = refCount;
		filename = fname;
	}
	public int versionNumber;
	public List<ChunkLocation> listOfLocations = new ArrayList<ChunkLocation>();
	public String chunkHash;
	public int referenceCount;
	public String filename;
	public int filenumber;
	public int index;
	public int size;	



	
	
}