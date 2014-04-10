package base;

import java.io.*;
import java.net.*;
import java.util.*;

import Utility.Message;
import Utility.Message.MessageType;
import base.ServerNode;

public class ClientServerNode extends ServerNode {
	String hostName = "68.181.174.149";
	int portNumber = 8111;

	public static void main(String args[]) throws Exception {

		TestInterface();
	}

	protected static void TestInterface() throws Exception {
		String userInput;
		BufferedReader stdIn = new BufferedReader (new InputStreamReader(System.in));
		while ((userInput = stdIn.readLine()) != null) {
			try {
				System.out
						.print("Please Enter the Test you want to run (Enter X to exit)");
				System.out.print("Enter parameters separated by a space");
				System.out.print("Example: Test1 7");
				String input = System.in.toString();
				String delim = "[ ]+";
				String[] tokens = input.split(delim);
				switch (tokens[0]) {
				case ("Test1"):
					break;
				case ("Test2"):
					break;
				case ("Test3"):
					break;
				case ("Test4"):
					break;
				case ("Test5"):
					break;
				case ("Test6"):
					break;
				case ("Test7"):
					break;
				case ("X"):
					break;
				default:
					throw new Exception();
				}
			} catch (Exception e) {
				System.out.println("That test doesn't exist; please try again");
			}
		}

	}

	public void msgEcho() {

		try (Socket echoSocket = new Socket(hostName, portNumber);
				PrintWriter out = new PrintWriter(echoSocket.getOutputStream(),
						true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						echoSocket.getInputStream()));
				BufferedReader stdIn = new BufferedReader(
						new InputStreamReader(System.in))) {

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
	public void test3(String filepath)
	{
		CDeleteDirectory(filepath);
	}

	public void CDeleteDirectory(String filepath)
	{
		//SENDING FILEPATH TO THE MASTER
		Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("config/config.properties"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(prop.getProperty("IP1"));
		try {
			Socket masterSocket = new Socket(prop.getProperty("IP1"), Integer.parseInt(prop.getProperty("PORT1")));
			ObjectOutputStream out = new ObjectOutputStream(masterSocket.getOutputStream());
			Message message = new Message(/*PARAM*/);
			message.type = MessageType.deleteDir;
			out.writeObject(message);
			out.close();
			masterSocket.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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