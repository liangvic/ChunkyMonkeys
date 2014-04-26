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
	serverType myType;
	//int targetPortNumber;	

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

		//if created new message, don't flip addressing data
		if (message.sender != myType){
			message.addressedTo = message.sender;
			message.sender = myType;
			message.receiverIP = message.senderIP;
			message.senderIP = myIP;
			message.receiverInputPort = message.senderInputPort;
			message.senderInputPort = myInputPortNumber;
		}
		System.out.println("Sending message "+message.type+ " to IP "+message.receiverIP);
		System.out.println("	Addressed to: "+message.addressedTo);
		System.out.println("	Sender: "+message.sender);
		System.out.println("	Reciever Ip "+message.receiverIP);
		System.out.println("	Sender Ip "+message.senderIP);
		System.out.println(message.receiverInputPort);
		System.out.println(message.senderInputPort);
		try(Socket outSocket =  new Socket(message.receiverIP, message.receiverInputPort );){
			ObjectOutputStream out = new ObjectOutputStream(outSocket.getOutputStream());
			out.writeObject(message);

		}
		catch (IOException e){
			System.err.println("Unable to send Message from " + myIP + " to " + message.receiverIP);
			e.printStackTrace();
		}

	}
	public void SendMessage(Message smessage, Socket socket) {
		//MESSAGE MUST HAVE IP and Socket Number

		//if created new message, don't flip addressing data
		System.out.println("Sending message to " + socket.getInetAddress() + " port " + socket.getLocalPort() + " " + socket.getPort());

		try
		{
			Message message = new Message(myIP,myType,myInputPortNumber,smessage.senderIP,smessage.sender,smessage.senderInputPort);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());			
			out.writeObject(message);
			out.flush();
			//out.close();			
		}
		catch (IOException e){
			System.err.println("Unable to send Message from " + myIP + " to " + smessage.senderIP);
			e.printStackTrace();
		}

	}
}

