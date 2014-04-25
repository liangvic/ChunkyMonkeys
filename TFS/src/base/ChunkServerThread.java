package base;

import java.net.Socket;

import Utility.HeartBeat;
import Utility.Message;
import Utility.SOSMessage;
import Utility.Message.msgType;
import Utility.Message.serverType;
import Utility.SOSMessage.msgTypeToServer;

public class ChunkServerThread extends ServerThread {
	ChunkServerNode server;

	public ChunkServerThread(ChunkServerNode sn, Socket s) {
		super(sn, s);
		server = sn;
	}
	
	public void DealWithMessage(Message message) {
		//if(!messageList.isEmpty()) {
		//	Message message = messageList.get(0);

			if(message instanceof HeartBeat)
			{
				server.PingMaster((HeartBeat)message);
			}
			else if(message instanceof SOSMessage)
			{
				if(((SOSMessage) message).msgToServer == msgTypeToServer.TO_SOSSERVER)
				{
					server.CheckVersionAfterStarting((SOSMessage)message);
				}
				else if (((SOSMessage) message).msgToServer == msgTypeToServer.TO_OTHERSERVER)
				{
					server.SendingDataToUpdateChunkServer((SOSMessage)message);
				}
				else if (((SOSMessage) message).msgToServer == msgTypeToServer.RECEIVINGDATA)
				{
					server.ReplacingData((SOSMessage)message);
				}
			}
			else if (message.type == msgType.DELETEDIRECTORY) {
				server.DeleteChunk(message.chunkClass);
			}

			else if (message.type == msgType.CREATEFILE) {
				server.AddNewBlankChunk(message);
			} else if (message.type == msgType.READFILE) {
				server.ReadChunks(message);
			} else if (message.type == msgType.APPENDTOFILE) {
				if (message.chunkClass == null) {
					System.out.println("chunkClass is null");
				}
				else if (message.type == msgType.CREATEFILE) {
					server.AddNewBlankChunk(message);
				} else if (message.type == msgType.READFILE) {
					server.ReadChunks(message);
				} else if (message.type == msgType.APPENDTOTFSFILE) {
					if(message.sender == serverType.MASTER) {
						System.out.println("Putting "+message.chunkClass.chunkHash+" into the map");
						server.chunkMap.put(message.chunkClass.chunkHash, message.chunkClass);
					}
					else if (message.sender == serverType.CLIENT) {
						System.out.println("Calling AppendToTSFFile Method");
						server.AppendToTFSFile(message);
					}
				} else if (message.type == msgType.COUNTFILES) {
					server.CountNumInFile(message.chunkClass);
				}
				else if (message.type == msgType.WRITETONEWFILE)
				{
					if (message.chunkClass == null) {
						System.out.println("chunkClass is null");
					}
					else
						server.WriteToNewFile(message);
				}
				messageList.remove(0);
			}
		}
	//}
}
