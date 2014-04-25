package Utility;

/**
 * 
 *
 */
public class ChunkLocation {
	public String chunkIP;
	public int chunkPort;
	public int byteOffset;
	public int fileNumber;//Starting from 0-4
	public ChunkLocation(String ip, int port)
	{
		chunkIP = ip;
		chunkPort = port;
	}
	
	public ChunkLocation(String ip, int port, 
			int byteOffset,int fileNumber) {
		chunkIP = ip;
		chunkPort = port;
		this.byteOffset = byteOffset;
		this.fileNumber = fileNumber;
	}
}
