package base;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Utility.Message;
import Utility.Message.msgType;
import Utility.Message.serverType;

public abstract class ServerThread extends Thread {
	ServerNode server;
	Socket socket;
	
	public ServerThread(ServerNode sn, Socket s) {
		server = sn;
		socket = s; 
		System.out.println("socket port:"+s.getPort());
	}
	
	public void run() {
		try {
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			Message incoming = (Message)in.readObject();
			
			if(incoming != null) {
				server.messageList.add(incoming);
				DealWithMessage(incoming);
			}
			
			//long time = System.currentTimeMillis();
            //in.close();
            //out.close();
           // System.out.println("Request processed: " + time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public abstract void DealWithMessage(Message message);

}
