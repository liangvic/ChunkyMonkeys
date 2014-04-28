package base;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Config;
import Utility.HeartBeat;
import Utility.HeartBeat.serverStatus;
import Utility.Message;
import Utility.NamespaceNode.lockType;
import Utility.NamespaceNode.nodeType;
import Utility.SOSMessage;
import Utility.SOSMessage.msgTypeToMaster;
import Utility.TFSLogger;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.NamespaceNode;
import Utility.lockInfo;

public class MasterServerNode extends ServerNode {
	Semaphore lockChange = new Semaphore(1, true);
	Semaphore fileWriteSemaphore = new Semaphore(1, true); //TODO;
	int operationID = 0;
	int chunksNeedToBeChecked = 0;
	Map<String, ChunkMetadata> chunkServerMap = Collections.synchronizedMap(new HashMap<String, ChunkMetadata>());
	Map<String, ServerData> ServerMap = new HashMap<String, ServerData>(); //TODO: make synchronized
	public static TFSLogger logger = new TFSLogger();
	String myIP;

	/**
	 * Inner class holding IP, port, ping/heartbeat, and serverStatus (DEAD or ALIVE) data
	 * Used in ServerMap
	 */
	public class ServerData {
		String IP;
		int clientPort;
		int serverPort;
		int TTL; // time to last ping - the last time it pinged master
		serverStatus status;

		public ServerData(String nIP, int nClientPort, int nServerPort) {
			IP = nIP;
			clientPort = nClientPort;
			serverPort = nServerPort;
			status = status.DEAD;
			TTL = 0;
		}
	}

	public MasterServerNode(String ip, int inPort) {
		super(ip, inPort);
		myType = serverType.MASTER;
		myIP = ip;
		
		LoadNamespaceMap();
		LoadServerData();
		if(NamespaceMap.size()>0)
		{
			LoadChunkServerMap();
		}
		else
		{
			//TODO: COMMENT OUT!
			System.out.println("NamespaceMap is empty so clearing out chunkserverMap just in case...");
			ClearChunkServerMapFile();
		}
		

	}

	/**
	 * @throws Exception
	 * Regularly check status of Chunk Servers (ALIVE or DEAD)
	 * If connection is established, start new thread to deal with message
	 */
	public void main() throws Exception {	
		toString();
		try (ServerSocket serverSocket = new ServerSocket(myInputPortNumber);)

		{
			//TODO: Put in timer to increase TTL and check on status of all servers in ServerMap
			Timer timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					for (Map.Entry<String, ServerData> entry : ServerMap.entrySet())
					{
						ServerData temp = entry.getValue();
						/*ServerData temp = entry.getValue();
						temp.TTL+=1;
						if (temp.TTL >= 10 && temp.status == serverStatus.ALIVE)
							temp.status = serverStatus.DEAD;*/

					}
				}
			}, 10000, 10000);

			while(true) { 
				Socket otherSocket = serverSocket.accept();

				System.out.println("Recieved Message from " + otherSocket.getInetAddress() + " Port " + otherSocket.getLocalPort());

				ServerThread st = new MasterServerThread(this, otherSocket);
				st.start();
			}


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

	/**
	 * 
	 */
	public void LoadChunkServerMap() {
		BufferedReader textReader = null;
		try {
			File file = new File("dataStorage/MData_ChunkServerMap.txt");
			FileReader fr = new FileReader(file);
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
				int newIndexCounter = 3 + (locationSize*4);
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
				String n_hash = data[newIndexCounter++];

				// count
				int n_count = Integer.parseInt(data[newIndexCounter++]);

				// filename
				String n_fileName = data[newIndexCounter++];

				// fileNumber
				int n_fileNumber = Integer.parseInt(data[newIndexCounter++]);

				// byteoffset//TODO: FIX BECAUSE NOT NEEDED ANYMORE
				//int n_byteOffset = Integer.parseInt(data[newIndexCounter++]);

				// index
				int n_index = Integer.parseInt(data[newIndexCounter++]);

				// size
				int n_size = Integer.parseInt(data[newIndexCounter++]);

				ChunkMetadata newMetaData = new ChunkMetadata(n_fileName,
						n_index, n_version, n_count);
				newMetaData.listOfLocations = locations;

				newMetaData.chunkHash = n_fileName + n_index;

				newMetaData.filenumber = n_fileNumber;
				//newMetaData.byteoffset = n_byteOffset;
				newMetaData.size = n_size;

				chunkServerMap.put(key, newMetaData);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				textReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void LoadNamespaceMap() {
		// String path = "dataStorage/MData_NamespaceMap.txt";
		BufferedReader textReader = null;
		try {
			File file = new File("dataStorage/MData_NamespaceMap.txt");
			FileReader fr = new FileReader(file);
			textReader = new BufferedReader(fr);

			String textLine;

			while ((textLine = textReader.readLine()) != null) {
				// STRUCTURE///
				// KEY TYPE CHILDLIST_SIZE CHILD CHILD CHILD 
				// LOCKLIST_SIZE LOCKSTATUS1 LOCKOPID1 ...
				// LOCKSTATUSN LOCKOPIDN 
				String[] data = textLine.split("\t");
				String key;
				List<String> children = new ArrayList<String>();
				List<lockInfo> tempLockList = new ArrayList<lockInfo>();
				key = data[0];
				String stringEnum = data[1];
				String lockEnum;
				nodeType type;
				lockType lType = null;
				int opID;
				if (stringEnum.equals("DIRECTORY")) {
					type = nodeType.DIRECTORY;
				} else {
					type = nodeType.FILE;
				}
				if(data.length > 2) //if there are children
				{
					for (int i = 3; i < 3 + Integer.parseInt(data[2]); i++) {
						children.add(data[i]);
					}
					
					if(data.length > 3 + Integer.parseInt(data[2])) //if there are locks
					{
						int newIndex = 3 + Integer.parseInt(data[2]);
						for (int i = newIndex + 1; i < newIndex + 1 + Integer.parseInt(data[newIndex])*2;i++)
						{
							lockEnum = data[i];
							if(lockEnum.equals("NONE"))
							{
								lType = lockType.NONE;
							}
							else if(lockEnum.equals("I_SHARED"))
							{
								lType = lockType.I_SHARED;
							}
							else if(lockEnum.equals("I_EXCLUSIVE"))
							{
								lType = lockType.I_EXCLUSIVE;
							}
							else if(lockEnum.equals("SHARED"))
							{
								lType = lockType.SHARED;
							}
							else if(lockEnum.equals("EXCLUSIVE"))
							{
								lType = lockType.EXCLUSIVE;
							}
							opID = Integer.parseInt(data[i+1]);
							tempLockList.add(new lockInfo(lType,opID));
						}
					}
				}
				NamespaceNode addingNode = new NamespaceNode(nodeType.DIRECTORY);
				addingNode.children = children;
				addingNode.type = type;
				addingNode.lockList = tempLockList;

				System.out.println("Adding " + key + " with children size " + addingNode.children.size());
				NamespaceMap.put(key, addingNode);

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				textReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void ClearChunkServerMapFile() {
		BufferedWriter out = null;
		try {
			File file = new File("dataStorage/MData_ChunkServerMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false); // true
			// tells
			// to
			// append
			// data.
			out = new BufferedWriter(fstream);
			// System.out.println("Writing out to file");
			out.write("");
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 */
	public void ClearNamespaceMapFile() {
		BufferedWriter out = null;
		try {
			File file = new File("dataStorage/MData_NamespaceMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false); // true
			// tells
			// to
			// append
			// data.
			out = new BufferedWriter(fstream);
			// System.out.println("Writing out to file");
			out.write("");
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void LoadServerData() {
		//TODO: change back to 5 as last boundary. changed to 6 for testing purposes
		for (int i = 2; i <= 5; i++) {
			String IP = Config.prop.getProperty("IP" + i);
			int clientPort = Integer.parseInt(Config.prop.getProperty("PORT"
					+ i + "_CLIENT_INPORT"));
			int serverPort = Integer.parseInt(Config.prop.getProperty("PORT"
					+ i + "_SERVER_INPORT"));
			ServerData temp = new ServerData(IP, clientPort, serverPort);
			ServerMap.put(IP, temp);
			System.out.print("Server at IP " + IP + " added to network:");
			System.out.println("ClientPort: " + clientPort + "\t ServerPort: "
					+ serverPort);
		}
	}
}

