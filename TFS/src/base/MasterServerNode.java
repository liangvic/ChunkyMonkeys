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

public class MasterServerNode extends ServerNode {
	Semaphore lockChange = new Semaphore(1, true);
	Semaphore fileWriteSemaphore = new Semaphore(1, true); //TODO;
	int operationID = 0;
	int chunksNeedToBeChecked = 0;
	Map<String, ChunkMetadata> chunkServerMap = new HashMap<String, ChunkMetadata>();
	Map<String, NamespaceNode> NamespaceMap = new HashMap<String, NamespaceNode>();
	Map<String, ServerData> ServerMap = new HashMap<String, ServerData>();
	TFSLogger tfsLogger = new TFSLogger();

	List<Message> messageList = Collections.synchronizedList(new ArrayList<Message>());


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
			status = null;
			TTL = 0;
		}
	}

	public MasterServerNode(String ip, int inPort, int outPort) {
		super(ip, inPort, outPort);
		myType = serverType.MASTER;

		LoadChunkServerMap();
		LoadNamespaceMap();
		LoadServerData();

	}

	// Don't call on this for now; using monolith structure
	/**
	 * @throws Exception
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
						temp.TTL+=1;
						if (temp.TTL >= 10 && temp.status == serverStatus.ALIVE)
							temp.status = serverStatus.DEAD;

					}
				}
			}, 10000, 10000);

			while(true) { 
				Socket otherSocket = serverSocket.accept();

				System.out.println("Recieved Messagr from " + otherSocket.getInetAddress() + " Port " + otherSocket.getLocalPort());

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
				// CHUNKLOCATION1_IP CHUNKLOCATION1_PORT ... CHUNKLOCATIONN_IP
				// CHUNKLOCATIONN_PORT
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
				for (int i = 3; i < newIndexCounter; i = i + 2) {
					locations.add(new ChunkLocation(data[i], Integer
							.parseInt(data[i + 1])));
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
				// KEY CHILD CHILD CHILD ...//
				String[] data = textLine.split("\t");
				String key;
				List<String> children = new ArrayList<String>();
				key = data[0];
				String stringEnum = data[1];
				nodeType type;
				if (stringEnum.equals("DIRECTORY")) {
					type = nodeType.DIRECTORY;
				} else {
					type = nodeType.FILE;
				}
				for (int i = 2; i < data.length; i++) {
					children.add(data[i]);
				}

				// TODO fix
				NamespaceNode addingNode = new NamespaceNode(nodeType.DIRECTORY);
				addingNode.children = children;
				addingNode.type = type;

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
		for (int i = 2; i <= 5; i++) {
			String IP = Config.prop.getProperty("IP" + i);
			int clientPort = Integer.parseInt(Config.prop.getProperty("PORT"
					+ i + "_CLIENT_INPORT"));
			int serverPort = Integer.parseInt(Config.prop.getProperty("PORT"
					+ i + "_SERVER_INPORT"));
			ServerData temp = new ServerData(IP, clientPort, serverPort);
			ServerMap.put(IP, temp);
			System.out.println("Server at IP " + IP + " added to network");
			System.out.println("ClientPort: " + clientPort + "\t ServerPort: "
					+ serverPort);
		}
	}
}

