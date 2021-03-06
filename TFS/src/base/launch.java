package base;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.TimerTask;

import Utility.ChunkMetadata;
import Utility.Config;
import Utility.NamespaceNode;
import Utility.TFSLogger;
import base.*;

import java.util.Timer;

public class launch {

	/**
	 * @param args
	 * @throws Exception
	 * Creates either MasterServerNode, ClientServerNode, or ChunkServerNode instance based on console input
	 * and passes in IP and port number based on matching config file value
	 * Calls proper main() method in either Master, Client, or Chunk Server
	 */
	public static void main(String args[]) throws Exception {

		/*Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			  @Override
			  public void run() {
				  for (Map.Entry<String, ChunkMetadata> entry : master.chunkServerMap.entrySet())
				  {
					  master.WritePersistentChunkServerMap(entry.getKey(),entry.getValue());
				  }
				  for (Map.Entry<String, NamespaceNode> entry : master.NamespaceMap.entrySet())
				  {
					  master.WritePersistentNamespaceMap(entry.getKey(),entry.getValue());
				  }
			  }
			}, 10000, 10000);*/
				
		
		System.out.println("1: Launch Master Server\n2: Launch Client\n3: Launch Chunk Server\n");
		Scanner a = new Scanner(System.in);
		int input = a.nextInt();
		
		if (input == 1){
			Config.prop.load(new FileInputStream("config/config.properties"));
			String myIP = Config.prop.getProperty("MASTERIP");
			int inPortNumber = Integer.parseInt(Config.prop.getProperty("MASTER_INPORT"));
			MasterServerNode master = new MasterServerNode(myIP, inPortNumber); 
			master.main();
		}
		else if (input == 2){
			System.out.println(" Enter config number (2-5)");
			input = a.nextInt();
			Config.prop.load(new FileInputStream("config/config.properties"));
			String IPkey = Config.prop.getProperty("IP" + Integer.toString(input));
			String inPortNumber = Config.prop.getProperty("PORT" + Integer.toString(input) + "_CLIENT_INPORT");
			ClientServerNode client = new ClientServerNode(IPkey, Integer.parseInt(inPortNumber));
			client.main();
		}
		else if (input == 3){
			System.out.println(" Enter config number (2-5)");
			input = a.nextInt();
			Config.prop.load(new FileInputStream("config/config.properties"));
			String IPkey = Config.prop.getProperty("IP" + Integer.toString(input));
			String inPortNumber = Config.prop.getProperty("PORT" + Integer.toString(input) + "_SERVER_INPORT");
			ChunkServerNode chunkServer = new ChunkServerNode(IPkey, Integer.parseInt(inPortNumber));
			chunkServer.main();
		}
		else{
			System.out.println("Invalid Selection");
		}
	}

}
