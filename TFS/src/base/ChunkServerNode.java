package base;

import Utility.ChunkMetadata;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Utility.ChunkMetadata;
import Utility.Message;
import Utility.ChunkLocation;
import Utility.NamespaceNode;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;

public class ChunkServerNode extends ServerNode {
	public ClientServerNode client;
	public MasterServerNode master;

	public class TFSFile {
		int fileNumber = 0;
		int spaceOccupied = 0;
		byte[] data = new byte[67108864];
	}

	List<TFSFile> file_list = new ArrayList<TFSFile>();

	public ChunkServerNode() {
		for (int i = 0; i < 5; i++)
			file_list.add(new TFSFile());

	}
	//hash to data

	Map<String, ChunkMetadata> chunkMap = new HashMap<String, ChunkMetadata>();	




	/* public static void main(String argv[]) throws Exception

    {

       ServerSocket welcomeSocket = new ServerSocket(6789);

       while(true)
       {
          Socket connectionSocket = welcomeSocket.accept();
          BufferedReader inFromClient =
             new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
          DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
          clientSentence = inFromClient.readLine();
          System.out.println("Received: " + clientSentence);
          capitalizedSentence = clientSentence.toUpperCase() + '\n';
          outToClient.writeBytes(capitalizedSentence);
       }
    }*/

	public void DealWithMessage(Message message)
	{
		if(message.type == msgType.DELETEDIRECTORY)
		{
			DeleteChunk(message.chunkClass);
		}

		else if (message.type == msgType.CREATEFILE)
		{
			AddNewBlankChunk(message.chunkClass);
		}
		else if (message.type == msgType.READFILE)
		{
			ReadChunks(message.chunkClass);
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
				byte[] dataINeed = new byte[metadata.size];
				//check byte offset
				for(int i=0;i<metadata.size;i++){
					dataINeed[i]=fileData.data[metadata.byteoffset+i];
				}
				client.DealWithMessage(new Message(msgType.PRINTFILEDATA ,dataINeed));
				break;
			}
		}

		//		client.DealWithMessage(new Message(msgType.PRINTFILEDATA, fileMetaData));

	}

	public void AddNewBlankChunk(ChunkMetadata metadata){
		//TODO: have to create new Chunkmetadata and copy over metadata
		chunkMap.put(metadata.filename, metadata);
		TFSFile current = file_list.get(metadata.filenumber);
		metadata.byteoffset = current.spaceOccupied;
		metadata.size = 0;
		current.data[current.spaceOccupied] = 1;
		current.data[current.spaceOccupied+1] = (byte) 232;
		current.data[current.spaceOccupied+2] = 1;
		current.spaceOccupied+= 3;
		
		for (int i = 0; i < current.spaceOccupied; i++){
			System.out.print("");
		}
		Message newMessage = new Message(msgType.CREATEDIRECTORY, metadata);
		newMessage.success = msgSuccess.REQUESTSUCCESS;
		master.DealWithMessage(newMessage);
		
		WritePersistentServerNodeMap(metadata.filename,metadata);
	}

	public void DeleteChunk(ChunkMetadata metadata)
	{
		ChunkMetadata chunkToDelete = null;
    	for (Map.Entry<String, ChunkMetadata> entry : chunkMap.entrySet())
		  {
    		if(entry.getValue().filename == metadata.filename)
    		{
    			for(TFSFile f: file_list)
    			{
    				if(f.fileNumber == entry.getValue().filenumber)
    				{
    					for(int i=0;i<entry.getValue().size;i++)
    					{
    						f.data[i+entry.getValue().byteoffset] = 0; //need to change later
    					}
    				}
    			}
    			chunkToDelete = metadata;
    			
    			Message successMessageToMaster = new Message(msgType.DELETEDIRECTORY);
        		successMessageToMaster.success = msgSuccess.REQUESTSUCCESS;
        		master.DealWithMessage(successMessageToMaster);
        		break;
    		}
		  }
    	
    	if(chunkToDelete != null)
    	{
    		chunkMap.remove(chunkToDelete);
    	}
    	
    		
    	
	}


	public void LoadServerNodeMap()
	{
		String path = "dataStorage/SData_ChunkMap.txt";
		try {
			FileReader fr = new FileReader(path);
			BufferedReader textReader = new BufferedReader(fr);
			String textLine;

			while((textLine = textReader.readLine())!= null)
			{
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
				String[] data = textLine.split("\t");

				//key
				String key;
				key = data[0];

				//version
				int n_version = Integer.parseInt(data[1]);

				//location
				List<ChunkLocation> locations = new ArrayList<ChunkLocation>();
				int locationSize = Integer.parseInt(data[2]);
				int newIndexCounter = 3 + (locationSize/2);
				for(int i=3; i<newIndexCounter; i=i+2)
				{
					locations.add(new ChunkLocation(data[i],Integer.parseInt(data[i+1])));
				}

				//hash
				/*List<Integer> hash = new ArrayList<Integer>();
				String n_tempHash = data[newIndexCounter++];
				for(int i=0;i<n_tempHash.length();i++)
				{
					hash.add(Character.getNumericValue(n_tempHash.charAt(i)));//adds at end
				}
				n_tempHash = hash.toString();*/
				
				//hash
				String n_hash = data[newIndexCounter++];

				//count
				int n_count = Integer.parseInt(data[newIndexCounter++]);

				//filename
				String n_fileName = data[newIndexCounter++];

				//fileNumber
				int n_fileNumber = Integer.parseInt(data[newIndexCounter++]);

				//byteoffset
				int n_byteOffset = Integer.parseInt(data[newIndexCounter++]);

				//index
				int n_index = Integer.parseInt(data[newIndexCounter++]);

				//size
				int n_size = Integer.parseInt(data[newIndexCounter++]);

				ChunkMetadata newMetaData = new ChunkMetadata(n_fileName,n_index,n_version,n_count);
				newMetaData.listOfLocations = locations;

				newMetaData.chunkHash = n_fileName+n_index;

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
		String fileToWriteTo = "dataStorage/File" + chunkmd.filenumber;
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
			File file = new File("dataStorage/MData_ChunkServerMap.txt");
			FileWriter fstream = new FileWriter(file.getAbsoluteFile(), true); //true tells to append data.
			out = new BufferedWriter(fstream);
			out.write(key+"\t"+chunkmd.versionNumber+"\t"+chunkmd.listOfLocations.size()+"\t");
			for(int i=0;i<chunkmd.listOfLocations.size();i++)
			{
				out.write(chunkmd.listOfLocations.get(i).chunkIP + "\t" + chunkmd.listOfLocations.get(i).chunkPort+ "\t");
			}
			out.write(chunkmd.chunkHash + "\t" +chunkmd.referenceCount + "\t" + chunkmd.filename + "\t");
			out.write(chunkmd.filenumber + "\t" + chunkmd.byteoffset + "\t" + chunkmd.index + "\t" + chunkmd.size);
			out.newLine();
		}
		catch (IOException e)
		{
			System.err.println("Error: " + e.getMessage());
		}

	}
}
