package base;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import Utility.Message;
import Utility.Message.serverType;

public class ServerNode {

	protected 
	String myIP;
	int myInputPortNumber; 
	static int myOutputPortNumber;
	serverType myType;
	//int targetPortNumber;	

	public ServerNode(String ip, int inPort, int outPort){
		myIP = ip;
		myInputPortNumber = inPort;
		myOutputPortNumber = outPort;
	}
	public String toString(){
		String type = "";
		if (myType == serverType.CHUNKSERVER) type = "ChunkServer";
		if (myType == serverType.MASTER) type = "Master";
		if (myType == serverType.CLIENT) type = "Client";
		type = type + ": IP " + myIP + " inputPort: " + myInputPortNumber + " outputPort: " + myOutputPortNumber;
		System.out.println(type);
		return type;
	}
	public void SendMessage(Message message) {
		//MESSAGE MUST HAVE IP and Socket Number

		//if created new message, don't flip addressing data
		if (message.sender != myType){
			message.addressedTo = message.sender;
			message.sender = myType;
			message.receiverIP = message.senderIP;
			message.senderIP = myIP;
			message.receiverInputPort = message.senderInputPort;
			message.senderInputPort = myInputPortNumber;
		}
		try(Socket outSocket =  new Socket(message.receiverIP, message.receiverInputPort );){
				ObjectOutputStream out = new ObjectOutputStream(outSocket.getOutputStream());
				out.writeObject(message);
//				out.close();			
		}
		catch (IOException e){
			System.err.println("Unable to send Message from " + myIP + " to " + message.receiverIP);
			e.printStackTrace();
		}

	}
}

