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
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Utility.ChunkLocation;
import Utility.ChunkMetadata;
import Utility.Message;
import Utility.NamespaceNode;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;

public class ChunkServerNode extends ServerNode {
	public ClientServerNode client;
	public MasterServerNode master;

	public class TFSFile {
		int fileNumber = 1;
		int spaceOccupied = 0;
		byte[] data = new byte[67108864];
		
		public TFSFile(int num){
			fileNumber = num;
			
		}
	}

	List<TFSFile> file_list = new ArrayList<TFSFile>();

	public ChunkServerNode() {
		for (int i = 1; i <= 5; i++){
			file_list.add(new TFSFile(i));
		}

	}

	// hash to data

	Map<String, ChunkMetadata> chunkMap = new HashMap<String, ChunkMetadata>();

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
	 * '\n'; outToClient.writeBytes(capitalizedSentence); } }
	 */

	public void DealWithMessage(Message message) {
		if (message.type == msgType.DELETEDIRECTORY) {
			DeleteChunk(message.chunkClass);
		}

		else if (message.type == msgType.CREATEFILE) {
			AddNewBlankChunk(message.chunkClass);
		} else if (message.type == msgType.READFILE) {
			ReadChunks(message.chunkClass);
		} else if (message.type == msgType.APPENDTOFILE) {
			if (message.chunkClass == null) {
				System.out.println("chunkClass is null");
			}
			else
				AppendToFile(message.chunkClass, message.fileData);
		} else if (message.type == msgType.COUNTFILES) {
			CountNumInFile(message.chunkClass);
		}
	}

	public void ReadChunks(ChunkMetadata metadata){
		//		List<List<Byte>> fileMetaData = new ArrayList<List<Byte>>();
		//		for(ChunkLocation messageLocation: metadata.listOfLocations){
		//			for(File fileData: file_list){
		//				if((fileData.location.chunkIP == messageLocation.chunkIP) && (fileData.location.chunkPort == messageLocation.chunkPort)){
		//					fileMetaData.add(fileData.data);
		//				}
		//			}
		//		}
		//		
		for(TFSFile fileData:file_list){
			if(metadata.filenumber == fileData.fileNumber){
				System.out.println("Reading from file number "+metadata.filenumber);
				System.out.println("Reading array size is "+metadata.size +" with byteoffset: "+metadata.byteoffset);
				System.out.println("File occupied space: "+fileData.spaceOccupied);
				try {
					String ogDecoded = new String(Arrays.copyOfRange(fileData.data, 1, 19), "UTF-8");
					System.out.println("about to read chunk. string reads: "+ogDecoded);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				byte[] dataINeed = new byte[metadata.size];
				// check byte offset
				int offSetIndex = metadata.byteoffset;
				for (int i = 0; i < metadata.size; i++) {
					dataINeed[i] = fileData.data[offSetIndex];
					offSetIndex++;
				}
				
				try {
					String decoded = new String(Arrays.copyOfRange(dataINeed, 0, 18), "UTF-8");
					System.out.println("just read chunk. string reads: "+decoded);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				client.DealWithMessage(new Message(msgType.PRINTFILEDATA ,dataINeed));

				break;
			}
		}

		// client.DealWithMessage(new Message(msgType.PRINTFILEDATA,
		// fileMetaData));

	}

	public void AddNewBlankChunk(ChunkMetadata metadata) {
		// TODO: have to create new Chunkmetadata and copy over metadata
		try{
		chunkMap.put(metadata.chunkHash, metadata);
		TFSFile current = file_list.get(1);
		metadata.byteoffset = current.spaceOccupied;

		metadata.size = 4;
		
		String s = "popo";
		byte buf[] = s.getBytes();	
		for (int i = 1; i <= s.length(); i++)
			file_list.get(1).data[current.spaceOccupied+ i] = buf[i-1];
		current.spaceOccupied += s.length();

		WritePersistentServerNodeMap(metadata.chunkHash, metadata);

		//WritePersistentServerFileData();
		}
		catch(Exception e){
			System.out.println("toobad");
			e.printStackTrace();
		}
		Message newMessage = new Message(msgType.CREATEDIRECTORY, metadata);
		newMessage.success = msgSuccess.REQUESTSUCCESS;
		master.DealWithMessage(newMessage);

		
	}


	public void AppendToFile(ChunkMetadata metadata, byte[] byteArray) {

		
		
		TFSFile current = new TFSFile(0);
		//Get the corresponding file number
		for(TFSFile tf:file_list){
			if(tf.fileNumber == metadata.filenumber)
				current = tf;
		}
		System.out.println("File #: "+current.fileNumber);
		System.out.println("Metadata correct file #: "+metadata.filenumber);
		
		current.data[current.spaceOccupied] = (byte) metadata.size;
		current.spaceOccupied++;
		System.out.println("occupied length: "+current.spaceOccupied);
		System.out.println("add length: "+byteArray.length);
		
		metadata.byteoffset = current.spaceOccupied;
		metadata.size = byteArray.length;
		
		
		for(int i=0;i<byteArray.length;i++){
			current.data[current.spaceOccupied] = byteArray[i];
			current.spaceOccupied++;
		}
		
//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		try { // appends byteArray to the end of current.data
//			
//			outputStream.write(current.data);
//			System.out.println(" output stream size is "+outputStream.size()+" now adding the new data");
//			outputStream.write(byteArray);
//			current.spaceOccupied+=byteArray.length;
//			System.out.println(" done.output stream size is now "+outputStream.size());
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		current.data = outputStream.toByteArray(); // does this create a new
//													// correct file or append(it
//													// shouldn't append)
//		current.spaceOccupied+=byteArray.length;
		current.data[current.spaceOccupied] = (byte) metadata.size;
		current.spaceOccupied++;
//		current.spaceOccupied = current.data.length;
		
		try {
			String decoded;
			decoded = new String(Arrays.copyOfRange(current.data, 1, 19), "UTF-8");
			System.out.println("String reads "+decoded+" -spacedoccupied is "+current.spaceOccupied );
			
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		chunkMap.put(metadata.chunkHash, metadata);
		
		Message newMessage = new Message(msgType.APPENDTOFILE, metadata);
		newMessage.success = msgSuccess.REQUESTSUCCESS;
		newMessage.addressedTo = serverType.MASTER;
		newMessage.sender = serverType.CHUNKSERVER;
		
		//appending on
		WritePersistentServerNodeMap(metadata.chunkHash,metadata);
		
		
		
		master.DealWithMessage(newMessage);
	}

	public void DeleteChunk(ChunkMetadata metadata) {
		String chunkToDelete = null;
    	for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
		  {
    		//System.out.println(entry.getValue().filename + " " + metadata.filename);
    		if(entry.getValue().chunkHash == metadata.chunkHash)
    		{
    			for(TFSFile f: file_list)
    			{
    				//System.out.println(entry.getValue().filenumber + " " + f.fileNumber);
    				if(f.fileNumber == entry.getValue().filenumber)
    				{
    					for(int i=0;i<entry.getValue().size;i++)
    					{
    						f.data[i+entry.getValue().byteoffset] = 0; //TODO:need to change later
    					}
    					f.spaceOccupied -= entry.getValue().size;
    				}
    			}
    			chunkToDelete = entry.getKey();
    			
    			Message successMessageToMaster = new Message(msgType.DELETEDIRECTORY);
        		successMessageToMaster.success = msgSuccess.REQUESTSUCCESS;
        		master.DealWithMessage(successMessageToMaster);
        		
        		break;
    		}
		  }
		if (chunkToDelete != null) {
			chunkMap.remove(chunkToDelete);
			
			ClearChunkMap();
			for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
			  {
				 WritePersistentServerNodeMap(entry.getKey(),entry.getValue());
			  }
		}
	}

	public void CountNumInFile(ChunkMetadata metadata)
	{
		int numCounted = 0;
		String chunkSizeString = null;
		int chunkSize;

		for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
		{
			if(entry.getValue().filename == metadata.filename)
			{
				for(TFSFile f: file_list)
				{
					if(f.fileNumber == entry.getValue().filenumber)
					{
						for(int i = 0;i<f.data.length; )
						{
							for(int j=i;j<4+i;j++)
							{
								chunkSizeString += (char)f.data[j];
							}
							chunkSize = Integer.parseInt(chunkSizeString);
							i += chunkSize;
							numCounted++;
						}
						
						Message successMessageToMaster = new Message(msgType.COUNTFILES);
						successMessageToMaster.success = msgSuccess.REQUESTSUCCESS;
						successMessageToMaster.countedLogicalFiles = numCounted;
						successMessageToMaster.filePath = metadata.filename;
						master.DealWithMessage(successMessageToMaster);
						break;
					}
				}
				break;
			}
		}

	}
	
/////////////////////////////WRITING TO PERSISTENT DATA///////////////////////////
	
	public void ClearChunkMap() {
		BufferedWriter out = null;
		try {
			File file = new File("dataStorage/SData_ChunkMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), false); // true
																				// tells
																				// to
																				// append
			// data.
			out = new BufferedWriter(fstream);
			//System.out.println("Writing out to file");
			out.write("");
			out.close();
		} catch (IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void LoadServerNodeMap() {
		String path = "dataStorage/SData_ChunkMap.txt";
		try {
			FileReader fr = new FileReader(path);
			BufferedReader textReader = new BufferedReader(fr);
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
			textReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public void WritePersistentServerNodeMap(String key, ChunkMetadata chunkmd)
	{
		//String fileToWriteTo = "dataStorage/File" + chunkmd.filenumber;
		//STRUCTURE///
		//KEY VERSION# SIZEOF_LOCATIONLIST 
		//CHUNKLOCATION1_IP CHUNKLOCATION1_PORT ... CHUNKLOCATIONN_IP CHUNKLOCATIONN_PORT
		//CHUNKHASH
		//REFERENCECOUNT
		//FILENAME
		//FILENUMBER
		//BYTEOFFSET
		//INDEX
		//SIZE
		BufferedWriter out = null;
		try  
		{
			File file = new File("dataStorage/SData_ChunkMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), true); //true tells to append data.

			out = new BufferedWriter(fstream);
			out.write(key + "\t" + chunkmd.versionNumber + "\t"
					+ chunkmd.listOfLocations.size() + "\t");
			for (int i = 0; i < chunkmd.listOfLocations.size(); i++) {
				out.write(chunkmd.listOfLocations.get(i).chunkIP + "\t"
						+ chunkmd.listOfLocations.get(i).chunkPort + "\t");
			}
			out.write(chunkmd.chunkHash + "\t" + chunkmd.referenceCount + "\t"
					+ chunkmd.filename + "\t");
			out.write(chunkmd.filenumber + "\t" + chunkmd.byteoffset + "\t"
					+ chunkmd.index + "\t" + chunkmd.size);
			out.newLine();

			out.close();
			fstream.close();
		}
		catch (IOException e)
		{
			System.err.println("Error: " + e.getMessage());
		}

	}

}
