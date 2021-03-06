package base;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Config;
import Utility.Message;
import Utility.Message.serverType;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.NamespaceNode;
import Utility.NamespaceNode.nodeType;
import base.ServerNode;

public class ClientServerNode extends ServerNode {

	Timer timer = new Timer();


	public ClientServerNode(String ip, int inPort)
	{
		super(ip, inPort);

		myType = serverType.CLIENT;
		masterIP = Config.prop.getProperty("MASTERIP");
		masterPort = Integer.parseInt(Config.prop.getProperty("MASTER_INPORT"));
	}

	String masterIP = null;
	int masterPort = 0;

	int chunkCountToExpect = 99; // TODO: remove
	int chunkReadsRecieved = 0; // TODO: remove
	List<Byte> readFileData = Collections.synchronizedList(new ArrayList<Byte>());


	/**
	 * @throws Exception
	 * Start thread for ConsoleThread to allow for console input to start Tests/Units
	 * If connection is established, start new thread to deal with message
	 */
	public void main() throws Exception {	
		toString();
		System.out.println("Try before");
		Scanner scan = new Scanner(System.in);
		try (ServerSocket mySocket = new ServerSocket(myInputPortNumber);)
		{

			ConsoleThread console = new ConsoleThread(this, scan);
			console.start();
			System.out.println("is socket closed? "+mySocket.isClosed());

			while(true) { 
				Socket otherSocket = mySocket.accept();
				System.out.println("My socket accepted");

				System.out.println("Recieved message from " + otherSocket.getInetAddress());

				ServerThread st = new ClientServerThread(this, otherSocket);
				st.start();
				//				TestInterface();



				/*ObjectInputStream in = new ObjectInputStream(otherSocket.getInputStream());

				ObjectInputStream in = new ObjectInputStream(otherSocket.getInputStream());

				Message incoming = (Message)in.readObject();
				System.out.println("got it " + incoming.senderIP);
				/*if(incoming != null) {
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




	/**
	 * @param dataMessage
	 * Print byte array data to local file 
	 */
	public void msgPrintFileData(Message dataMessage) {
		chunkReadsRecieved++;
		//hard coded
		//		chunkCountToExpect = 2;
		for (byte b : dataMessage.fileData)
			readFileData.add(b);
		System.out.print(dataMessage.localFilePath);
		//System.out.print(localPathToCreateFile);
		if (chunkReadsRecieved == chunkCountToExpect) {
			System.out.println("Client: recieved all "+chunkCountToExpect+ " chunks. Now writing file");
			System.out.print(dataMessage.fileData);
			byte[] finalByteArray = new byte[readFileData.size()];
			synchronized(readFileData) {
				for (int n = 0; n < readFileData.size(); n++)
					finalByteArray[n] = readFileData.get(n);
			}
			try {
				//File file = new File(localPathToCreateFile);
				File file = new File(dataMessage.localFilePath);
				file.createNewFile();
				//FileOutputStream fileOuputStream = new FileOutputStream(localPathToCreateFile);
				FileOutputStream fileOuputStream = new FileOutputStream(dataMessage.localFilePath);
				fileOuputStream.write(dataMessage.fileData);//finalByteArray);
				fileOuputStream.close();

				System.out.println("Done");
			} catch (Exception e) {
				e.printStackTrace();
			}
			chunkCountToExpect = 99;
			chunkReadsRecieved = 0;
			readFileData.clear();
		}

	}



	/**
	 * @param filepath
	 * @param nFiles
	 */
	public void test2(String filepath, int nFiles) {
		if (NamespaceMap.get(filepath) != null) {
			if (NamespaceMap.get(filepath).children.size() > 0) {
				System.out.println("Finding the children!");
				List<String> childs = NamespaceMap.get(filepath).children;
				for (int a = 0; a < childs.size(); a++) {
					test2helper(childs.get(a), nFiles);
				}
			}
		}

		for (int i = 1; i <= nFiles; i++) {
			try {
				CCreateFile(filepath, (String) ("File" + i));
			} catch (Exception e) {
				System.out.println("Unable to create files");
			}
		}


	}


	/**
	 * @param filepath
	 * @param nFiles
	 */
	public void test2helper(String filepath, int nFiles) {
		String filename = "File";
		if (NamespaceMap.get(filepath) != null) {
			if (NamespaceMap.get(filepath).type != nodeType.FILE) {
				if (NamespaceMap.get(filepath).children.size() > 0) {
					List<String> childs = NamespaceMap.get(filepath).children;
					for (int a = 0; a < childs.size(); a++) {
						test2helper(childs.get(a), nFiles);
					}
				}
			}

			for (int i = 1; i <= nFiles; i++) {
				try {
					CCreateFile(filepath, (String) (filename + i));
				} catch (Exception e) {
					System.out.println("Unable to create files");
				}
			}
		}


	}

	/**
	 * @param folderFilepath
	 * @param fileName
	 */
	public void CCreateFile(String folderFilepath, String fileName) {
		Message message = new Message(myIP,myType,myInputPortNumber,masterIP,serverType.MASTER,masterPort);
		message.type = msgType.CREATEFILE;
		message.filePath = folderFilepath;
		message.chunkindex = 1;
		message.fileName = fileName;
		message.addressedTo = serverType.MASTER;
		message.sender = serverType.CLIENT;
		System.out.println("Sending message to create file");
		try {
			SendMessageToMaster(message);
		} catch (Exception e) {
			System.out.println("Unable to send message");
		}
	}

	/**
	 * @param filepath
	 */
	public void test3(String filepath) {
		CDeleteDirectory(filepath);
	}

	/**
	 * @param m
	 */
	public void msgRequestAReadToChunkserver(Message m) {
		m.sender = myType;
		m.senderIP = myIP;
		m.addressedTo = serverType.CHUNKSERVER;
		m.senderInputPort = myInputPortNumber;
		m.receiverIP = m.chunkClass.listOfLocations.get(0).chunkIP;
		m.receiverInputPort = m.chunkClass.listOfLocations.get(0).chunkPort;
		SendMessageToChunkServer(m);
	}


	/**
	 * @param filepath
	 */
	public void CDeleteDirectory(String filepath) {
		Message message = new Message(myIP,myType,myInputPortNumber,masterIP,serverType.MASTER,masterPort);
		message.type = msgType.DELETEDIRECTORY;
		message.filePath = filepath;
		message.sender = serverType.CLIENT;
		SendMessageToMaster(message);

	}


	public void unit1(int NumFolders, int numSubDirectories){
		List<String> queue = new ArrayList<String>();
		CCreateDirectory("1");
		System.out.println("Creating 1");

		String parentfilepath = "1";
		int folderName = 2;
		String newfilepath = parentfilepath + "\\" + folderName;
		int subDirectoryCounter = 0;
		while(folderName<=NumFolders){
			subDirectoryCounter++;
			if(subDirectoryCounter>numSubDirectories){
				subDirectoryCounter=1;
				//				parentfilepath = newfilepath;
				parentfilepath = queue.get(0);
				for(int i=1;i<queue.size();i++){
					queue.set(i-1,  queue.get(i));

				}
				queue.remove(queue.size()-1);
			}

			newfilepath = parentfilepath + "\\" + folderName;

			NamespaceNode nn = new NamespaceNode(nodeType.DIRECTORY);
			NamespaceMap.put(newfilepath, nn);
			System.out.println("Added " + newfilepath + "to the map");
			if(NamespaceMap.containsKey(parentfilepath))
			{
				NamespaceMap.get(parentfilepath).children.add(newfilepath);
			}
			System.out.println("Added " + newfilepath + "to " + parentfilepath + "as child");


			final String finalFilePath = newfilepath;
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					CCreateDirectory(finalFilePath);
				}

			}, 1000);
			CCreateDirectory(newfilepath);
			System.out.println("Creating "+newfilepath);
			queue.add(newfilepath);
			folderName++;



		}

	}


	/**
	 * @param filepath
	 */
	public void CCreateDirectory(String filepath) {
		Message message = new Message(myIP,myType,myInputPortNumber,masterIP,serverType.MASTER,masterPort);
		message.type = msgType.CREATEDIRECTORY;
		message.filePath = filepath;
		message.sender = serverType.CLIENT;
		SendMessageToMaster(message);
	}


	/**
	 * @param NumFolders
	 */
	public void test1(final int NumFolders) {
		Timer timer = new Timer();
		final int count = 1;
		CCreateDirectory("1");
		NamespaceMap.put("1", new NamespaceNode(nodeType.DIRECTORY));

		if (NumFolders > 1) {
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					helper("1", count * 2, NumFolders);
				}


			}, 1000);
		}
		if (NumFolders > 2) {
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					helper("1", count * 2 + 1, NumFolders);
				}
			},1000);
		}


	}

	/**
	 * @param parentfilepath
	 * @param folderName
	 * @param NumMaxFolders
	 */
	public void helper(String parentfilepath, final int folderName, final int NumMaxFolders) {
		if (folderName <= NumMaxFolders) {
			Timer timer = new Timer();
			final String newfilepath = parentfilepath + "\\" + folderName;

			NamespaceNode nn = new NamespaceNode(nodeType.DIRECTORY);
			NamespaceMap.put(newfilepath, nn);
			System.out.println("Added " + newfilepath + " to the map");
			if(NamespaceMap.containsKey(parentfilepath))
			{
				NamespaceMap.get(parentfilepath).children.add(newfilepath);
			}

			CCreateDirectory(newfilepath);


			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					helper(newfilepath, folderName * 2, NumMaxFolders);
					helper(newfilepath, folderName * 2 + 1, NumMaxFolders);
				}

			}, 1000);
		}

	}

	/**
	 * @param parentfilepath
	 * @param folderName
	 * @param NumMaxFolders
	 * @param NumSubdirectories
	 */
	public void unit1helper(String parentfilepath, int folderName, int NumMaxFolders, int NumSubdirectories){
		if(folderName<=NumMaxFolders){
			for(int i=1;i<=NumSubdirectories;i++){
				String newfilepath = parentfilepath + "\\" + folderName+1;
				CCreateDirectory(newfilepath);
			}
		}

		if (folderName <= NumMaxFolders) {
			String newfilepath = parentfilepath + "\\" + folderName;
			CCreateDirectory(newfilepath);
			helper(newfilepath, folderName * 2, NumMaxFolders);
			helper(newfilepath, folderName * 2 + 1, NumMaxFolders);
		}
	}

	/**
	 * @param fullFilePath
	 */
	public void CCreateFile(String fullFilePath) { // including filename
		Message msg = new Message(myIP,myType,myInputPortNumber,masterIP,serverType.MASTER,masterPort);
		int index = fullFilePath.lastIndexOf('\\');

		msg.chunkindex = 1;
		msg.type = msgType.CREATEFILE;
		msg.fileName = fullFilePath.substring(index + 1);
		msg.filePath = fullFilePath.substring(0, index);
		SendMessageToMaster(msg);
	}


	/**
	 * @param fullFilePath
	 * @param byteStream
	 * @return
	 */
	public ChunkMetadata RetrieveMetadata(String fullFilePath, byte[] byteStream, int numReplicas){
		System.out.println("Attempting to retrieve metadata for: "+fullFilePath);		
		Message msg = new Message(myIP,myType,myInputPortNumber,masterIP,serverType.MASTER,masterPort);
		int index = fullFilePath.lastIndexOf('\\');
		msg.type = msgType.WRITETONEWFILE;
		msg.fileData = byteStream;
		msg.fileName = fullFilePath.substring(index+1);
		msg.filePath = fullFilePath.substring(0, index);
		msg.addressedTo = serverType.MASTER;
		msg.sender = serverType.CLIENT;
		msg.replicaCount = numReplicas;

		//TODO: FIX THE TWO LINES DIRECTLY BELOW. NOT THE ONE AFTER IT
		return null;
		//return master.AssignChunkServer(msg);
		/////master.DealWithMessage(msg);
	}


	/**
	 * @param cm
	 * @param fullFilePath
	 * @param byteStream
	 */
	public void CAppendToFile(ChunkMetadata cm, String fullFilePath, byte[] byteStream){

		//CAppendToFile(fullFilePath, byteStream);//retrieve metadata

		/*Message msg = new Message(msgType.APPENDTOFILE, byteStream);
		int index = fullFilePath.lastIndexOf('\\');
		msg.type = msgType.APPENDTOFILE;
		msg.fileData = byteStream;
		msg.fileName = fullFilePath.substring(index+1);
		msg.filePath = fullFilePath.substring(0, index);
		msg.addressedTo = serverType.CHUNKSERVER;
		msg.sender = serverType.CLIENT;
		msg.chunkClass= cm;

		SendMessageToChunkServer(msg);*/
	}

	/**
	 * @param fullFilePath
	 * @param byteStream
	 * @return
	 */
	//	public ChunkMetadata RetrieveMetadata(String fullFilePath, byte[] byteStream, int numReplicas){
	//		System.out.println("Attempting to retrieve metadata for: "+fullFilePath);		
	//		Message msg = new Message(msgType.WRITETONEWFILE, byteStream);
	//		int index = fullFilePath.lastIndexOf('\\');
	//		msg.fileName = fullFilePath.substring(index+1);
	//		msg.filePath = fullFilePath.substring(0, index);
	//		msg.addressedTo = serverType.MASTER;
	//		msg.sender = serverType.CLIENT;
	//		msg.replicaCount = numReplicas;
	//		return master.AssignChunkServer(msg);
	//		//master.DealWithMessage(msg);
	//	}

	/**
	 * @param localPath
	 * @param fullFilePath
	 */
	public void CWriteToNewFile(String localPath, String fullFilePath, int numberOfReplicas){

		if (fullFilePath == null || localPath == null) {
			System.out.println("FilePath or localPath are null values, please reenter query");
			return;
		}

		byte[] byteFile = convertFileToBytes(localPath);

		//Message msg = new Message(msgType.WRITETONEWFILE, byteFile);
		Message msg = new Message(myIP,myType,myInputPortNumber,masterIP,serverType.MASTER,masterPort);
		int index = fullFilePath.lastIndexOf('\\');
		msg.type = msgType.WRITETONEWFILE;
		msg.fileData = byteFile;
		msg.fileName = fullFilePath.substring(index+1);
		msg.filePath = fullFilePath.substring(0, index);
		msg.addressedTo = serverType.MASTER;
		msg.sender = serverType.CLIENT;
		msg.replicaCount = numberOfReplicas;
		//Going to ask the master to populate the chunk metadata
		SendMessageToMaster(msg);
		System.out.println("Sent to master. End of write to new file part 1/2");	

	}

	public void CWriteToNewFile2(Message msg){
		System.out.println("Starting write to new file part 2/2");
		if (msg.chunkClass == null)
		{
			System.out.println("ERROR: " + msg.filePath+ " already exists.");
		}
		else
		{
			System.out.println("New chunkmetadata hash "+ msg.chunkClass.chunkHash);
			msg.addressedTo = serverType.CHUNKSERVER;
			msg.sender = myType;
			msg.senderInputPort = myInputPortNumber;
			msg.senderIP = myIP;



			System.out.println("Writing chunks to "+msg.chunkClass.listOfLocations.size()+" replica(s)");
			for(int j=0;j<msg.chunkClass.listOfLocations.size();j++){
				msg.receiverIP = msg.chunkClass.listOfLocations.get(j).chunkIP;
				msg.receiverInputPort = msg.chunkClass.listOfLocations.get(j).chunkPort;
				//Testing Hack
				//msg.receiverIP = "68.181.174.61";
				//msg.receiverInputPort = 7070;
				System.out.println("Sending message to ip: "+msg.receiverIP+" port: "+msg.receiverInputPort);
				SendMessageToChunkServer(msg);
			}
		}
	}

	/**
	 * @param hashstring
	 * @param myServer
	 */
	public void AppendToChunkServer(String hashstring, ChunkServerNode myServer){
		//later on set chunk handle and chunkserver to myServer
		//CAppendToFile2(filePath, byteFile);
	}

	/**
	 * @param localPath
	 * @return
	 */
	public byte[] convertFileToBytes(String localPath)
	{
		FileInputStream fileInputStream = null;
		File localFile = new File(localPath);

		byte[] byteFile = new byte[(int) localFile.length()];

		// convert file into array of bytes
		try {
			fileInputStream = new FileInputStream(localFile);
			fileInputStream.read(byteFile);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return byteFile;
	}

	public void unit4(String localPath, String filePath, int numberOfReplicas){
		if(numberOfReplicas<1 || numberOfReplicas>4){
			System.out.println("Invalid replica number!");
		}else
			CWriteToNewFile(localPath, filePath,numberOfReplicas);
	}

	// Test 4 stores a file on the local machine in a target TFS specified by
	// its filepath
	/**
	 * @param localPath
	 * @param filePath
	 */
	public void test4(String localPath, String filePath) {

		CWriteToNewFile(localPath, filePath,1);

	}
	//Future test4 reference:
	/*
			//separate into 64MB chunks
			if (byteFile.length >67108864){ //67108864 bytes = 64MB
				int numChunks = ((byteFile.length - 1)/67108864) + 1;
				int currentIndex = 0;
				byte[][] Chunks = new byte[numChunks][]; //creates a list of byte arrays
				while(currentIndex <= numChunks-1){
					Chunks[currentIndex] = Arrays.copyOf(byteFile, 67108864);
					//Still incomplete
				}	
			}
			String masterIP = "68.181.174.149";
			int masterPort = 8111;
			try {
				Socket masterSocket = new Socket(masterIP, masterPort);
				PrintWriter out = new PrintWriter(masterSocket.getOutputStream(),
						true);
				// BufferedReader in = new BufferedReader(new
				// InputStreamReader(echoSocket.getInputStream()));
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(
						System.in));
			} catch (UnknownHostException e) {
				System.err.println("Don't know about host " + masterIP);
				System.exit(1);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Couldn't get I/O for the connection to "
						+ masterIP);
				System.exit(1);
			}

			 Step 2: Receive Message to be Written
			 if it exists, return error. else read content and store in TFS file
			 Pseudocode referring to
			 coderanch.com/t/205325/sockets/java/send-java-Object-socket
	 */
	//end of future test4 reference

	/**
	 * @param filePath
	 * @param localPath
	 */
	public void test5(String filePath, String localPath) {
		//Check if inputs are NULL
		if (filePath == null || localPath == null) {
			System.out.println("FilePath or localPath are null values, please reenter query");
			return;
		}

		Message m = new Message(myIP,myType,myInputPortNumber,masterIP,serverType.MASTER,masterPort);
		m.type = msgType.READFILE;
		m.filePath = filePath;
		m.localFilePath = localPath;
		m.sender = serverType.CLIENT;
		SendMessageToMaster(m);

		// Step 1 connect to the master
		// String masterIP = "68.181.174.149";
		// int masterPort = 8111;
		//
		// try {
		// Socket masterSocket = new Socket(masterIP, masterPort);
		// ObjectOutputStream objOut = new
		// ObjectOutputStream(masterSocket.getOutputStream());
		// // PrintWriter out = new PrintWriter(masterSocket.getOutputStream(),
		// true);
		// // BufferedReader in = new BufferedReader(new
		// InputStreamReader(echoSocket.getInputStream()));
		// BufferedReader stdIn = new BufferedReader( new
		// InputStreamReader(System.in));
		// //Step 2 Create a message
		// Message m = new Message(msgType.READFILE ,filePath);
		// //Step 3 Write to the master server
		// objOut.writeObject(m);
		// } catch (UnknownHostException e) {
		// System.err.println("Don't know about host " + masterIP);
		// System.exit(1);
		// } catch (IOException e) {
		// e.printStackTrace();
		// System.err.println("Couldn't get I/O for the connection to " +
		// masterIP);
		// System.exit(1);
		// }
	}

	/**
	 * @param localPath
	 * @param filePath
	 */
	public void test6(String localPath, String filePath){
		CAppendToTFSFile(localPath, filePath);
	}

	/**
	 * @param localPath
	 * @param filePath
	 */
	public void unit6(String localPath, String filePath){
		CAppendToTFSFile(localPath, filePath);
	}

	/**
	 * Retrieves chunkClass from Master, which contains chunk locations of all replicas
	 * @param localPath
	 * @param filePath
	 */
	public void CAppendToTFSFile(String localPath, String filePath){
		int index = filePath.lastIndexOf('\\');
		byte[] byteArray = null; //WTF IS THIS SHIT
		Message m = new Message(myIP,myType,myInputPortNumber,masterIP,serverType.MASTER,masterPort);
		m.type = msgType.APPENDTOTFSFILE;
		m.filePath = filePath;
		m.fileName = filePath.substring(index + 1);
		m.localFilePath = localPath;
		m.replicaCount = 3;
		m.fileData = byteArray; //WHY ASSIGN NULL GODDAM IT
		m.sender = serverType.CLIENT;
		SendMessageToMaster(m);
	}

	/**
	 * Runs through all chunk locations with replica present and asks them to append data
	 * @param message
	 */
	public void AppendToAllReplicas(Message message)
	{
		System.out.println("Appendtoallreplicas");
		for (ChunkLocation loc : message.chunkClass.listOfLocations)
		{		
			System.out.println("Tying to read local file on ip "+loc.chunkIP+ " on port "+loc.chunkPort);
			
			Message m = new Message(myIP, myType, myInputPortNumber, loc.chunkIP, serverType.CHUNKSERVER, loc.chunkPort);
			m.type = msgType.APPENDTOTFSFILE;
			m.filePath = message.filePath;
			m.fileName = message.fileName;
			m.localFilePath = message.localFilePath;
			m.chunkClass = message.chunkClass;
			System.out.println("Printing byte offsets");
			for(ChunkLocation cl:message.chunkClass.listOfLocations){
				System.out.println(cl.byteOffset);
			}
			ReadLocalFile(m); //this should send to individual chunkserver
		}
	}

	/**
	 * @param message
	 */
	public void ReadLocalFile(Message message) {
		System.out.println("ReadLocalFile");
		FileInputStream fileInputStream = null;
		File file = new File(message.localFilePath);
		byte[] byteFile = new byte[(int) file.length()];

		// convert file into array of bytes
		System.out.println("convert file to array of bytes");
		try {
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(byteFile);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
//		ChunkMetadata cm = message.chunkClass;	
		String decodedString = "string";
		try {
			decodedString = new String(byteFile, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("writing bytefile: "+byteFile + " string is "+decodedString);
		/*cm = RetrieveMetadata(filePath, byteFile); //sends message to master to append to specified file
		//now chunkServer will be set
		System.out.println("metadata hash "+cm.chunkHash);*/
		//Message msg = new Message(msgType.APPENDTOTFSFILE, byteFile);
		message.type = msgType.APPENDTOTFSFILE;
		message.fileData = byteFile;
		message.addressedTo = serverType.CHUNKSERVER;
		message.sender = serverType.CLIENT;
//		message.chunkClass = cm;
		System.out.println(message.localFilePath);
		System.out.println(message.chunkClass.size);
		message.chunkClass.size = (int) file.length();
		SendMessageToChunkServer(message);
	}
	public void printCommands(){
		System.out.println("Format closely follows that of in the Assignment Page");
		System.out.println("Test1 <numfolders>				i.e. Test1 7");
		System.out.println("Unit1 <numfolders> <fanout>			i.e. Unit1 7 3");
		System.out.println("Test2/Unit2 <filepath> <numfiles>		i.e. Test2 1\\2 3");
		System.out.println("Test3/Unit3 <filepath> 				i.e. Unit3 1\\3");
		System.out.println("Test4 <local> <remote filepath> 		i.e. Test4 C:\\MyDocuments\\Image.png 1\\File1.png");
		System.out.println("Unit4 <local> <remote filepath> <num replicas>	i.e. Unit4 C:\\MyDocuments\\Image.png 1\\File1.png 2");
		System.out.println("Test5 <remote filepath> <local>			i.e. Test5 1\\File1.png C:\\MyDocument\\Pic.png");		
		System.out.println("Test6 <local> <TFS filepath> 			i.e. Test6 C:\\MyDocument\\Pic.png 1\\File1.png");
		System.out.println("Test7 <TFSfile>(use .haystack entension) 	i.e. Test7 Picture.haystack");
	}
	public void ExpectChunkNumberForRead(int i) {
		System.out.println("Client: Expecting "+i+" chunks");
		chunkCountToExpect = i;
	}

	/**
	 * @param filepath
	 */
	public void test7(String filepath)
	{
		System.out.println("Test7 Path: "+filepath);
		Message m = new Message(myIP,myType,myInputPortNumber,masterIP,serverType.MASTER,masterPort);
		m.type = msgType.COUNTFILES;
		m.filePath = filepath;
		m.sender = serverType.CLIENT;
		SendMessageToMaster(m);
	}

	/**
	 * @param message
	 */
	public void SendMessageToChunkServer(Message message) {
		SendMessage(message);
	}

	/**
	 * @param message
	 */
	public void SendMessageToMaster(Message message) {	
		SendMessage(message);

	}
}
