package base;

import java.io.*;
import java.net.*;
import java.util.*;

import Utility.Message;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import base.ServerNode;

public class ClientServerNode extends ServerNode {
	public MasterServerNode master;
	public ChunkServerNode chunkServer;
	
	String hostName = "68.181.174.149";
	int portNumber = 8111;


	protected void TestInterface() throws Exception {
		Scanner a = new Scanner(System.in);
		String input;
		do {
			System.out.print("Please Enter the Test you want to run (Enter X to exit)\n");
			System.out.print("Enter parameters separated by a space\n");
			System.out.print("Example: Test1 7\n");
			input = a.nextLine();
			
			String delim = "[ ]+";
			String[] tokens = input.split(delim);
			try {
				switch (tokens[0]) {
				case ("Test1"):
					if (tokens.length == 2)
						test1(Integer.parseInt(tokens[1]));
					else
						throw new Exception();
					break;
				case ("Test2"):
					if (tokens.length == 3)
						test2(tokens[1],Integer.parseInt(tokens[2]));
					else
						throw new Exception();					
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
				System.out.println("Invalid OP or Parameters. \n");
			}
		} while (input != "X" || input != "x");

	}

	public void DealWithMessage(Message message)
	{
		if(message.type == msgType.DELETEDIRECTORY)
		{
			if(message.success == msgSuccess.SUCCESS)
			{
				System.out.println("Deleted directory sucessfully!");
			}
			else
			{
				System.out.println("Error! Couldn't delete directory...");
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
			System.err.println("Couldn't get I/O for the connection to "
					+ hostName);
			System.exit(1);
		}
	}
	
	public void test2(String filepath, int nFiles) {
		// Create N files in a directory and its subdirectories until the leaf
		// subdirectories.
		// Each file in a directory is named File1, File2, ..., FileN

		Socket sock;
		try {
			sock = new Socket(hostName, portNumber);
			ObjectOutputStream out = new ObjectOutputStream(
					sock.getOutputStream());
			for (int i = 0; i < nFiles; ++i) {
				Message message = new Message(msgType.CREATEFILE, filepath);
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
	public void test3(String filepath) {
		CDeleteDirectory(filepath);
	}

	public void CDeleteDirectory(String filepath) {
		// SENDING FILEPATH TO THE MASTER
		/*Properties prop = new Properties();
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
			Message message = new Message(msgType.DELETEDIRECTORY);
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
		}*/
		Message message = new Message(msgType.DELETEDIRECTORY);
		master.DealWithMessage(message);

	}

	/*public void test1(Integer numOfDir, String filepath) // recursively creates the specified num of
										// directories
	{
		Socket sock;
		try {
			Properties prop = new Properties();
			prop.load(new FileInputStream("config/config.properties"));
			sock = new Socket(prop.getProperty("MASTERIP"), Integer.parseInt(prop.getProperty("MASTERPORT")));
			ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
			for(int i = 0; i < numOfDir; ++i) {
				Message message = new Message(Integer.toString(i+1), msgType.CREATEDIRECTORY);
				out.writeObject(message);
			}
			//out.close();
			//sock.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/
	
	public void CCreateDirectory(String filepath)
	{
		Message message = new Message(msgType.CREATEDIRECTORY, filepath);
		master.DealWithMessage(message);
	}
	
	public void test1(String filepath, int NumFolders){
		int count = 1;
		if (NumFolders > 1)
			helper(filepath, count*2, NumFolders);
		if (NumFolders > 2)
			helper(filepath, count*2+1, NumFolders);
	}
	
	public void helper(String parentfilepath, int folderName, int NumMaxFolders){		
		if (folderName <= NumMaxFolders){
			String newfilepath1 = parentfilepath + File.pathSeparator + folderName;
			CCreateDirectory(newfilepath1);
			helper (newfilepath1, folderName*2, NumMaxFolders);
		}
		if (folderName + 1 <= NumMaxFolders){
			String newfilepath2 = parentfilepath + File.pathSeparator + folderName;
			CCreateDirectory(newfilepath2);
			helper (newfilepath2, folderName*2 + 1, NumMaxFolders);
		}
	}
	
	//Test 4 stores a file on the local machine in  a target TFS specified by its filepath
	public void test4(String localPath, String filePath){
		//Step 1: Connect to the Master
		String masterIP = "68.181.174.149";
		int masterPort = 8111;
		
		try{
			Socket masterSocket = new Socket(masterIP, masterPort);
			PrintWriter out =  new PrintWriter(masterSocket.getOutputStream(), true);
	        // BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
	        BufferedReader stdIn = new BufferedReader(  new InputStreamReader(System.in));
	        
		} catch (UnknownHostException e){
			System.err.println("Don't know about host " + masterIP);
			System.exit(1);
		} catch (IOException e) {
        	e.printStackTrace();
            System.err.println("Couldn't get I/O for the connection to " +
            		masterIP);
            System.exit(1);
        }
		//Step 2: Receive Message to be Written
		//if it exists, return error. else read content and store in TFS file
		//Pseudocode referring to coderanch.com/t/205325/sockets/java/send-java-Object-socket
		//InputStream is = clientSocket.getInputStream();
		//ObjectInputStream ois = new ObjectInputStream(is);
		//receivedMsg rmsg = (Message)rmsg.readObject();
	}
	
	public void test5(String filePath, String localPath){
		//Step 1 connect to the master
		 	String masterIP = "68.181.174.149";
	        int masterPort = 8111;
	 
	        try {
	            Socket masterSocket = new Socket(masterIP, masterPort);
	        	ObjectOutputStream objOut = new ObjectOutputStream(masterSocket.getOutputStream());
//	            PrintWriter out =  new PrintWriter(masterSocket.getOutputStream(), true);
	           // BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
	            BufferedReader stdIn = new BufferedReader(  new InputStreamReader(System.in));
	          //Step 2 Create a message
		        Message m = new Message(msgType.READFILE ,filePath);
		      //Step 3 Write to the master server
		        objOut.writeObject(m);
	        } catch (UnknownHostException e) {
	            System.err.println("Don't know about host " + masterIP);
	            System.exit(1);
	        } catch (IOException e) {
	        	e.printStackTrace();
	            System.err.println("Couldn't get I/O for the connection to " +
	            		masterIP);
	            System.exit(1);
	        }
	        
	    
	    
	   //Step 4 recieves the master message
	        //Step 5 send a request to the chunkserver
	}
}