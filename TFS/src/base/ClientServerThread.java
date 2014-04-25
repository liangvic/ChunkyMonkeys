package base;

import java.net.Socket;

import Utility.Message;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;

public class ClientServerThread extends ServerThread {
	ClientServerNode server;

	public ClientServerThread(ClientServerNode sn, Socket s) {
		super(sn, s);
		server = sn;
	}

	public void DealWithMessage() {
		if(!messageList.isEmpty()) {
			Message message = messageList.get(0);
			if (message.type == msgType.DELETEDIRECTORY) {
				if (message.success == msgSuccess.REQUESTSUCCESS) {
					System.out.println("Deleted directory sucessfully!");
				} else {
					System.out.println("Error! Couldn't delete directory...");
				}
			}
			else if (message.type == msgType.CREATEDIRECTORY) {
				if(message.success == msgSuccess.REQUESTSUCCESS) {
					System.out.println("Successfully created directory "+message.filePath);
				}
				else {
					System.out.println("Failed to create directory "+message.filePath);
				}
			}
			else if (message.type == msgType.CREATEFILE) {
				if(message.success == msgSuccess.REQUESTSUCCESS) {
					System.out.println("Successfully created file "+message.filePath);
				}
				else {
					System.out.println("Failed to create file "+message.filePath);
				}
			}
			else if(message.type == msgType.READFILE)
			{
				if(message.success == msgSuccess.REQUESTSUCCESS) {
					System.out.println("Read succeeded. Found file.");
					server.msgRequestAReadToChunkserver(message);
				}
				else {
					System.out.println("Read failed. Could not find file.");
				}
				// Supposedly going to cache it. Implementation will be completed
				// later.lol
				// uses the location to contact the chunkserver

			} else if (message.type == msgType.PRINTFILEDATA) {
				server.msgPrintFileData(message);
			} else if (message.type == msgType.APPENDTOTFSFILE) {
				if(message.sender == serverType.MASTER)
				{
					if(message.success == msgSuccess.REQUESTERROR)
					{
						//server.CWriteToNewFile(server.localPathToReadFile, message.filePath, 3);
						server.CWriteToNewFile(message.localFilePath, message.filePath, 3);
					}
					else
					{
						server.AppendToAllReplicas(message);
					}
				}
				//ReadLocalFile(message);
			}else if (message.type == msgType.EXPECTEDNUMCHUNKREAD) {
				server.ExpectChunkNumberForRead(message.expectNumChunkForRead);
			} else if (message.type == msgType.PRINTFILEDATA) {
				server.msgPrintFileData(message);
			} else if (message.type == msgType.APPENDTOTFSFILE) {
				server.ReadLocalFile(message);
			}else if (message.type == msgType.EXPECTEDNUMCHUNKREAD) {
				server.ExpectChunkNumberForRead(message.expectNumChunkForRead);
			} else if (message.type == msgType.WRITETONEWFILE) {
				server.CWriteToNewFile2(message);

				// Supposedly going to cache it. Implementation will be completed
				// later.lol
				// uses the location to contact the chunkserver
			}
			messageList.remove(0);
		}
	}
}
