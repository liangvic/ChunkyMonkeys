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
		
		client.TestInterface();
		
		/* System.out.println("1: Launch Master Server\n2: Launch Client Server\n");
		Scanner a = new Scanner(System.in);
		int input = a.nextInt();
		
		if (input == 1){
			MasterServerNode master = new MasterServerNode(); 
			master.myIP = Config.prop.getProperty("MASTERIP");
			master.myPortNumber = Config.prop.getProperty("MASTERPORT");
		}
		else if (input == 2 || input == 3 || input == 4 || input == 5 ){
			client.TestInterface();
			String IPkey = Config.prop.getProperty("IP" + input);
			String portKeyClient = Config.prop.getProperty("PORT" + input + "_CLIENT");
			String portKeyServer = Config.prop.getProperty("PORT" + input + "_SERVER")
			ClientServerNode client = new ClientServerNode(); 
			ChunkServerNode chunkServer = new ChunkServerNode();
		}
		else{
			System.out.println("Invalid Selection");
		}*/
	}

}
