package base;

import java.io.*;
import java.util.Scanner;

import base.*;
public class launch {

	public static void main(String args[]) throws Exception {
		MasterServerNode master = new MasterServerNode();
		ClientServerNode client = new ClientServerNode();
		ChunkServerNode chunkServer = new ChunkServerNode();
		
		master.client = client;
		master.chunkServer = chunkServer;
		client.master = master;
		client.chunkServer = chunkServer;
		chunkServer.master = master;
		chunkServer.client = client;
		
		client.TestInterface();
		
		/*System.out.println("1: Launch Master Server\n2: Launch Client Server\n");
		Scanner a = new Scanner(System.in);
		int input = a.nextInt();
		
		if (input == 1){
			MasterServerNode master = new MasterServerNode(); 
		}
		else if (input == 2 ){
			ClientServerNode client = new ClientServerNode(); 
			client.TestInterface();
		}
		else{
			System.out.println("Invalid Selection");
		}*/
	}

}
