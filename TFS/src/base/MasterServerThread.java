package base;

import java.net.Socket;

import Utility.HeartBeat;
import Utility.Message;
import Utility.SOSMessage;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.SOSMessage.msgTypeToMaster;

public class MasterServerThread extends ServerThread {
	MasterServerNode server;

	public MasterServerThread(MasterServerNode sn, Socket s) {
		super(sn, s);
	}

	public void DealWithMessage() {
		while(!messageList.isEmpty()) {
			Message inputMessage = messageList.get(0);
			server.operationID++; //used to differentiate operations
			System.out.println("inputMessagetype "+ inputMessage.type);
			if(inputMessage instanceof HeartBeat)
			{

			}
			else if(inputMessage instanceof SOSMessage)
			{
				if(((SOSMessage)inputMessage).msgToMaster == msgTypeToMaster.REQUESTINGDATA)
				{
					server.TellOtherChunkServerToSendData((SOSMessage)inputMessage);
				}
				else if(((SOSMessage)inputMessage).msgToMaster == msgTypeToMaster.DONESENDING)
				{
					//TODO: finished with the sending of data -- release semaphore-kind of thing?
				}
			}
			else if (inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CLIENT) {
				server.MDeleteDirectory(inputMessage, server.operationID);
			} else if (inputMessage.type == msgType.DELETEDIRECTORY && inputMessage.sender == serverType.CHUNKSERVER) {
				server.RemoveParentLocks(inputMessage.filePath);
				if (inputMessage.success == msgSuccess.REQUESTSUCCESS) {
					// SendSuccessMessageToClient();
				} else {
					// SendErrorMessageToClient();
				}
			} else if (inputMessage.type == msgType.CREATEDIRECTORY) {
				if (inputMessage.sender == serverType.CLIENT) {
					try {
						server.CreateDirectory(inputMessage, server.operationID);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (inputMessage.sender == serverType.CHUNKSERVER) {
					server.RemoveParentLocks(inputMessage.filePath);
					if (inputMessage.success == msgSuccess.REQUESTSUCCESS) {
						// SendSuccessMessageToClient();
					} else {
						// SendErrorMessageToClient();
					}
				} 
			}
			else if (inputMessage.type == msgType.CREATEFILE) {
				if (inputMessage.sender == serverType.CLIENT)
					server.CreateFile(inputMessage, server.operationID);
				else if (inputMessage.sender == serverType.CHUNKSERVER) {
					server.RemoveParentLocks(inputMessage.filePath);
					if (inputMessage.success == msgSuccess.REQUESTSUCCESS)
						System.out.println("File "
								+ inputMessage.chunkClass.filename
								+ " creation successful");
					else if (inputMessage.success == msgSuccess.REQUESTERROR)
						System.out.println("File "
								+ inputMessage.chunkClass.filename
								+ " creation failed");
				}
			} else if (inputMessage.type == msgType.READFILE) {
				if(inputMessage.sender == serverType.CLIENT)
				{
					server.ReadFile(inputMessage, server.operationID);
				}
				else if (inputMessage.sender == serverType.CHUNKSERVER)
				{
					server.RemoveParentLocks(inputMessage.filePath);
					//TODO: NEED TO ADD IN FURTHER IF STATEMENTS
				}
			}

			else if (inputMessage.type == msgType.CREATEFILE) {
				if (inputMessage.sender == serverType.CLIENT)
					server.CreateFile(inputMessage, server.operationID);
				else if (inputMessage.sender == serverType.CHUNKSERVER) {
					server.RemoveParentLocks(inputMessage.filePath);
					if (inputMessage.success == msgSuccess.REQUESTSUCCESS)
						System.out.println("File "
								+ inputMessage.chunkClass.filename
								+ " creation successful");
					else if (inputMessage.success == msgSuccess.REQUESTERROR)
						System.out.println("File "
								+ inputMessage.chunkClass.filename
								+ " creation failed");
				}
			} else if (inputMessage.type == msgType.READFILE) {
				if(inputMessage.sender == serverType.CLIENT)
				{
					server.ReadFile(inputMessage, server.operationID);
				}
				else if (inputMessage.sender == serverType.CHUNKSERVER)
				{
					server.RemoveParentLocks(inputMessage.filePath);
					//TODO: NEED TO ADD IN FURTHER IF STATEMENTS
				}
			}
			else if(inputMessage.type == msgType.APPENDTOFILE)
			{
				if(inputMessage.sender == serverType.CLIENT)
					server.AssignChunkServer(inputMessage);//, operationID);
				else if (inputMessage.sender == serverType.CHUNKSERVER){
					server.RemoveParentLocks(inputMessage.filePath);
					if(inputMessage.success == msgSuccess.REQUESTSUCCESS){
						System.out.println("File "+ inputMessage.chunkClass.filename + " creation successful");
					}
					else if (inputMessage.success == msgSuccess.REQUESTERROR)
						System.out.println("File " + inputMessage.chunkClass.filename + " creation failed");
				}
			}
			else if(inputMessage.type == msgType.APPENDTOTFSFILE) // Test 6
			{
				if(inputMessage.sender == serverType.CLIENT) {
					//should retrieve ip and port for chunkserver who has the filepath here
					server.AppendToTFSFile(inputMessage, server.operationID);
				}
				else if(inputMessage.sender == serverType.CHUNKSERVER) {
					server.RemoveParentLocks(inputMessage.filePath);
					if(inputMessage.success == msgSuccess.REQUESTSUCCESS){
						System.out.println("File "+ inputMessage.chunkClass.filename + " append successful");
					}
				}
			}else if(inputMessage.type == msgType.WRITETONEWFILE) // Test 4 & Unit 4
			{
				server.AssignChunkServer(inputMessage);
			}	
			/*
				else if (inputMessage.type == msgType.APPENDTOTFSFILE) // Test 6
				{

					FindFile(inputMessage.filePath, operationID);
				}
				else if (inputMessage.sender == serverType.CHUNKSERVER)
				{
					RemoveParentLocks(inputMessage.filePath);
					System.out.println("There are " + inputMessage.countedLogicalFiles + " logical files in " + inputMessage.filePath);
				}*/

			messageList.remove(0);
		}
	}
}
