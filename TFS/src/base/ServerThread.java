package base;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Utility.Message;

public abstract class ServerThread extends Thread {
	ServerNode server;
	Socket socket;
	List<Message> messageList = Collections.synchronizedList(new ArrayList<Message>());
	
	public ServerThread(ServerNode sn, Socket s) {
		server = sn;
		socket = s; 
	}
	
	public void run() {
		try {
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			Message incoming = (Message)in.readObject();
			
			if(incoming != null) {
				messageList.add(incoming);
				DealWithMessage(incoming);
			}
			
			long time = System.currentTimeMillis();
            //in.close();
            System.out.println("Request processed: " + time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void DealWithMessage(Message message) {
	}
	public void SendMessage(Message message) {
		//MESSAGE MUST HAVE IP and Socket Number

		//if created new message, don't flip addressing data

		try{
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			out.writeObject(message);
			out.flush();
			//out.close();			
		}
		catch (IOException e){
			System.err.println("Unable to send Message from " + server.myIP + " to " + message.receiverIP);
			e.printStackTrace();
		}

	}
}
