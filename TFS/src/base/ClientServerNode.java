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
	
	public void test5(String filePath, String localPath){
		//Step 1 connect to the master
		 	String masterIP = "68.181.174.149";
	        int masterPort = 8111;
	 
	        try (
	            Socket masterSocket = new Socket(masterIP, masterPort);
	            PrintWriter out =  new PrintWriter(masterSocket.getOutputStream(), true);
	           // BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
	            BufferedReader stdIn = new BufferedReader(  new InputStreamReader(System.in))
	        ){
	            String userInput;
//	            while ((userInput = stdIn.readLine()) != null) {
//	                out.println(userInput);
//	                System.out.println("echo: " + in.readLine());
//	            }
	        } catch (UnknownHostException e) {
	            System.err.println("Don't know about host " + masterIP);
	            System.exit(1);
	        } catch (IOException e) {
	        	e.printStackTrace();
	            System.err.println("Couldn't get I/O for the connection to " +
	            		masterIP);
	            System.exit(1);
	        }
	        
	    //Step 2 Create a message
	        Message m = new Message(filePath);
	    //Step 3 Write to the master server
	   //Step 4 recieves the master message
	        //Step 5 send a request to the chunkserver
	}
}