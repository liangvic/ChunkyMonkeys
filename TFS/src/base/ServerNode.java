package base;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Utility.Message;
import Utility.NamespaceNode;
import Utility.Message.serverType;

public class ServerNode {

	protected 
	String myIP;
	int myInputPortNumber; 
	serverType myType;
	//int targetPortNumber;	

	Map<String, NamespaceNode> NamespaceMap = new HashMap<String, NamespaceNode>();
	List<Message> messageList = Collections.synchronizedList(new ArrayList<Message>());
	
	public ServerNode(String ip, int inPort){
		myIP = ip;
		myInputPortNumber = inPort;
	}
	public String toString(){
		String type = "";
		if (myType == serverType.CHUNKSERVER) type = "ChunkServer";
		if (myType == serverType.MASTER) type = "Master";
		if (myType == serverType.CLIENT) type = "Client";
		type = type + ": IP " + myIP + " inputPort: " + myInputPortNumber;
		System.out.println(type);
		return type;
	}
	public void SendMessage(Message message) {
		//MESSAGE MUST HAVE IP and Socket Number

		//sender is set to myself or ip is myself already
		if (!(message.sender == myType && message.senderIP == myIP)){
			message.addressedTo = message.sender;
			message.sender = myType;
			message.receiverIP = message.senderIP;
			message.senderIP = myIP;
			message.receiverInputPort = message.senderInputPort;
			message.senderInputPort = myInputPortNumber;
		}
		System.out.println("Sending message "+message.type+ " to "+message.receiverIP);
		try(Socket outSocket =  new Socket(message.receiverIP, message.receiverInputPort );){
				ObjectOutputStream out = new ObjectOutputStream(outSocket.getOutputStream());
				out.writeObject(message);

				out.flush();

		}
		catch (IOException e){
			System.err.println("Unable to send Message from " + myIP + " to " + message.receiverIP);
			e.printStackTrace();
		}

	}

}

