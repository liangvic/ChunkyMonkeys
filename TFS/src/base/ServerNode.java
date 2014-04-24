package base;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import Utility.Message;
import Utility.Message.serverType;

public class ServerNode {

	protected 
	static	String myIP;
	static int myPortNumber;  
	static serverType myType;
	//int targetPortNumber;	

	public void SendMessage(Message message) {
		//MESSAGE MUST HAVE IP and Socket Number

		//if created new message, don't flip addressing data
		if (message.sender != myType){
			message.addressedTo = message.sender;
			message.sender = myType;
			message.receiverIP = message.senderIP;
			message.senderIP = myIP;
			message.recieverPort = message.senderPort;
			message.senderPort = myPortNumber;
		}

		try(Socket serverSocket =  new Socket(message.receiverIP, message.recieverPort);)
		{

			ObjectOutputStream out = new ObjectOutputStream(serverSocket.getOutputStream());
			out.writeObject(message);
			out.close();
		}
		catch (IOException e){
			System.err.println("Unable to send Message from " + myIP + " to " + message.senderIP);
			e.printStackTrace();
		}

	}
}

