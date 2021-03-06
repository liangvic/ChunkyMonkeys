package base;

import java.io.File;
import java.net.Socket;

import Utility.Message;
import Utility.NamespaceNode;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.NamespaceNode.nodeType;

public class ClientServerThread extends ServerThread {
	ClientServerNode server;

	public ClientServerThread(ClientServerNode sn, Socket s) {
		super(sn, s);
		server = sn;
	}

	/**
	 * Schedules proper action for Client Server according message 
	 */
	public void DealWithMessage(Message message) {
		System.out.println("YOU GOT MESSAGE. Type = "+message.type.toString());
//		if(!messageList.isEmpty()) {
//			messageList.add(message);
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
					/*NamespaceNode nn = new NamespaceNode(nodeType.DIRECTORY);
					server.NamespaceMap.put(message.filePath, nn);
					File file = new File(message.filePath);
					server.NamespaceMap.get(file.getParent()).children.add(message.filePath);*/
				}
				else {
					System.out.println("Failed to create directory "+message.filePath);
				}
			}
			else if (message.type == msgType.CREATEFILE) {
				if(message.success == msgSuccess.REQUESTSUCCESS) {
					System.out.println("Successfully created file "+message.filePath);
					/*NamespaceNode nn = new NamespaceNode(nodeType.FILE);
					server.NamespaceMap.put(message.filePath, nn);
					File file = new File(message.filePath);
					server.NamespaceMap.get(file.getParent()).children.add(message.filePath);*/
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
						System.out.println("Message request error");
						//server.CWriteToNewFile(server.localPathToReadFile, message.filePath, 3);
						server.CWriteToNewFile(message.localFilePath, message.filePath, 3);
					}
					else
					{
						System.out.println("Message request success");
						server.AppendToAllReplicas(message);
					}
				}
				//ReadLocalFile(message);
			}else if (message.type == msgType.EXPECTEDNUMCHUNKREAD) {
				server.ExpectChunkNumberForRead(message.expectNumChunkForRead);
			} else if (message.type == msgType.PRINTFILEDATA) {
				server.msgPrintFileData(message);
			} else if (message.type == msgType.WRITETONEWFILE) {
				System.out.println("Got the return message from client!");
				server.CWriteToNewFile2(message);

				// Supposedly going to cache it. Implementation will be completed
				// later.lol
				// uses the location to contact the chunkserver
			}
			server.messageList.remove(message);
		}
//	}
}
