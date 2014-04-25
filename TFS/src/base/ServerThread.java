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
	Socket socket;
	List<Message> messageList = Collections.synchronizedList(new ArrayList<Message>());
	
	public ServerThread(Socket s) {
		socket = s; 
	}
	
	public void run() {
		try {
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

			Message incoming = (Message)in.readObject();
			
			if(incoming != null) {
				messageList.add(incoming);
				DealWithMessage(incoming);
			}
			
			long time = System.currentTimeMillis();
            //in.close();
            //out.close();
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
}
