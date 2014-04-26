package base;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.WriteAbortedException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import base.MasterServerNode.ServerData;
import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Config;
import Utility.HeartBeat;
import Utility.HeartBeat.serverStatus;
import Utility.Message;
import Utility.NamespaceNode;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.SOSMessage;
import Utility.SOSMessage.msgTypeToMaster;
import Utility.SOSMessage.msgTypeToServer;
import Utility.TFSFile;

public class ChunkServerNode extends ServerNode {
	//public ClientServerNode client;
	//public MasterServerNode master;

	List<TFSFile> file_list = Collections.synchronizedList(new ArrayList<TFSFile>());
	// hash to data
	Map<String, ChunkMetadata> chunkMap = Collections.synchronizedMap(new HashMap<String, ChunkMetadata>());
	
	
	public ChunkServerNode(String ip, int inPort) {
		super(ip, inPort);
		
		myType = serverType.CHUNKSERVER;
		masterIP = Config.prop.getProperty("MASTERIP");
		masterPort = Integer.parseInt(Config.prop.getProperty("MASTER_INPORT"));
		synchronized(file_list)
		{
			for (int i = 0; i <= 4; i++){
				file_list.add(new TFSFile(i));
			}
		}
		
		LoadServerNodeMap();
		LoadFileData();
	}

	String masterIP = null;
	int masterPort = 0;

	/*
	 * public static void main(String argv[]) throws Exception
	 * 
	 * {
	 * 
	 * ServerSocket welcomeSocket = new ServerSocket(6789);
	 * 
	 * while(true) { Socket connectionSocket = welcomeSocket.accept();
	 * BufferedReader inFromClient = new BufferedReader(new
	 * InputStreamReader(connectionSocket.getInputStream())); DataOutputStream
	 * outToClient = new DataOutputStream(connectionSocket.getOutputStream());
	 * clientSentence = inFromClient.readLine(); System.out.println("Received: "
	 * + clientSentence); capitalizedSentence = clientSentence.toUpperCase() +
	 * '\n'; outToClient.writeBytes(capitalizedSentence); }
	 * 
	 *  //TODO:Timer that send out pings at regular intervals
	 * }
	 */

	/**
	 * @throws Exception
	 */
	public void main() throws Exception {	
		toString();
		try (ServerSocket mySocket = new ServerSocket(myInputPortNumber);)

		{
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
						HeartBeat HBMessage = new HeartBeat(myIP, myType, myInputPortNumber, 
								masterIP,serverType.MASTER, masterPort,serverStatus.ALIVE);
						SendMessage(HBMessage);
				}
			}, 10000, 10000);
			
			while(true) { 
				Socket otherSocket = mySocket.accept();
				ServerThread st = new ChunkServerThread(this, otherSocket);
				st.start();
				/*ObjectInputStream in = new ObjectInputStream(otherSocket.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(otherSocket.getOutputStream());
				Message incoming = (Message)in.readObject();
				if(incoming != null) {
					messageList.add(incoming);
					DealWithMessage();
					//outToClient.writeBytes(capitalizedSentence); 
				}*/
			}

			//TODO: Put in timer to increase TTL and check on status of all servers in ServerMap
			//TODO: Deal with Server Pings
			//TODO: Send updated chunkserver data to re-connected servers
		}
		catch (IOException e) {
			System.out
			.println("Exception caught when trying to listen on port "
					+ myInputPortNumber + " or listening for a connection");
			System.out.println(e.getMessage());
		}
		finally{

		}
	}

	

	/////////////////////////////WRITING TO PERSISTENT DATA///////////////////////////

	

	/**
	 * 
	 */
	public void LoadServerNodeMap() {
		String path = "dataStorage/SData_ChunkMap.txt";
		BufferedReader textReader = null;
		try {
			FileReader fr = new FileReader(path);
			textReader = new BufferedReader(fr);
			String textLine;

			while ((textLine = textReader.readLine()) != null) {
				// STRUCTURE///
				// KEY VERSION# SIZEOF_LOCATIONLIST
				// CHUNKLOCATION1_IP CHUNKLOCATION1_PORT 
				// CHUNKLOCATION1_BYTEOFFSET CHUNKLOCATION1_FILENUMBER
				//... CHUNKLOCATIONN_IP CHUNKLOCATIONN_PORT
				// CHUNKHASH
				// REFERENCECOUNT
				// FILENAME
				// FILENUMBER
				// BYTEOFFSET
				// INDEX
				// SIZE
				String[] data = textLine.split("\t");

				// key
				String key;
				key = data[0];

				// version
				int n_version = Integer.parseInt(data[1]);

				// location
				List<ChunkLocation> locations = new ArrayList<ChunkLocation>();
				int locationSize = Integer.parseInt(data[2]);
				int newIndexCounter = 3 + (locationSize / 2);
				for (int i = 3; i < newIndexCounter; i = i + 4) {
					locations.add(new ChunkLocation(data[i], Integer
							.parseInt(data[i + 1]),Integer.parseInt(data[i+2]),
							Integer.parseInt(data[i+3])));
				}

				// hash
				/*
				 * List<Integer> hash = new ArrayList<Integer>(); String
				 * n_tempHash = data[newIndexCounter++]; for(int
				 * i=0;i<n_tempHash.length();i++) {
				 * hash.add(Character.getNumericValue
				 * (n_tempHash.charAt(i)));//adds at end } n_tempHash =
				 * hash.toString();
				 */

				// hash
				String n_hash = data[newIndexCounter++];

				// count
				int n_count = Integer.parseInt(data[newIndexCounter++]);

				// filename
				String n_fileName = data[newIndexCounter++];

				// fileNumber
				int n_fileNumber = Integer.parseInt(data[newIndexCounter++]);

				// byteoffset
				int n_byteOffset = Integer.parseInt(data[newIndexCounter++]);

				// index
				int n_index = Integer.parseInt(data[newIndexCounter++]);

				// size
				int n_size = Integer.parseInt(data[newIndexCounter++]);

				ChunkMetadata newMetaData = new ChunkMetadata(n_fileName,
						n_index, n_version, n_count);
				newMetaData.listOfLocations = locations;

				newMetaData.chunkHash = n_fileName + n_index;

				newMetaData.filenumber = n_fileNumber;
				newMetaData.byteoffset = n_byteOffset;
				newMetaData.size = n_size;
				chunkMap.put(key, newMetaData);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try {
				textReader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void LoadFileData()
	{
		synchronized(chunkMap)
		{
			for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet()) {		
				TFSFile fileToStoreInto = file_list.get(entry.getValue().filenumber);
				String path = "dataStorage/File" + entry.getValue().filenumber;

				try {
					Path path1 = Paths.get(path);
					byte[] testData = new byte[entry.getValue().size+8];
					testData = Files.readAllBytes(path1);
					byte[] fileSize = new byte[4];
					for(int i = 0; i<4;i++)
					{
						fileSize[i] = testData[entry.getValue().byteoffset + i];
					}
					fileToStoreInto.spaceOccupied = java.nio.ByteBuffer.wrap(fileSize).getInt();
					byte[] data = new byte[entry.getValue().size];
					for(int i = 4; i<entry.getValue().size-4;i++)
					{
						data[i-4] = testData[entry.getValue().byteoffset+i];
					}
					fileToStoreInto.data = data;


				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

		

	

}
