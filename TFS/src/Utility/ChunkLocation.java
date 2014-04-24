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
	public ChunkLocation(String ip, int port) {
		chunkIP = ip;
		chunkPort = port;
	}
}
