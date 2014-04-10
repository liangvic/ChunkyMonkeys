package base;

import java.io.*;
import java.net.*;
import java.util.*;

import Utility.Message;
import base.ServerNode;

public class ClientServerNode extends ServerNode {
	public static void main(String args[]) throws Exception {

	        String hostName = "68.181.174.149";
	        int portNumber = 8111;
	 
	        try (
	            Socket echoSocket = new Socket(hostName, portNumber);
	            PrintWriter out =
	                new PrintWriter(echoSocket.getOutputStream(), true);
	            BufferedReader in =
	                new BufferedReader(
	                    new InputStreamReader(echoSocket.getInputStream()));
	            BufferedReader stdIn =
	                new BufferedReader(
	                    new InputStreamReader(System.in))
	        ) {
	            String userInput;
	            while ((userInput = stdIn.readLine()) != null) {
	                out.println(userInput);
	                System.out.println("echo: " + in.readLine());
	            }
	        } catch (UnknownHostException e) {
	            System.err.println("Don't know about host " + hostName);
	            System.exit(1);
	        } catch (IOException e) {
	        	e.printStackTrace();
	            System.err.println("Couldn't get I/O for the connection to " +
	                hostName);
	            System.exit(1);
	        }
	}
	public void test1(Integer numOfDir) // creates the specified num of directories
	{
		Socket sock;
		try {
			sock = new Socket(myIP, myPortNumber);
			ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
			for(int i = 0; i < numOfDir; ++i) {
				Message message = new Message(/*PARAM*/);
				out.writeObject(message);
			}
			out.close();
			sock.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}