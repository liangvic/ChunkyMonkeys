package Utility;

public class TFSFile {
	public int fileNumber = 1;
	public int spaceOccupied = 0;
	public byte[] data = new byte[67108864];

	public TFSFile(int num){
		fileNumber = num;

	}
}
