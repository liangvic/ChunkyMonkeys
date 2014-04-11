package base;

import java.io.*;
import java.net.*;
import java.util.*;

import Utility.Message;
import Utility.Message.serverType;
import Utility.Message.msgSuccess;
import Utility.Message.msgType;
import Utility.NamespaceNode;
import Utility.NamespaceNode.nodeType;
import base.ServerNode;

public class ClientServerNode extends ServerNode {
	public MasterServerNode master;
	public ChunkServerNode chunkServer;

	int chunkCountToExpect = 99;
	int chunkReadsRecieved = 0;
	List<Byte> readFileData = new ArrayList<Byte>();
	String localPathToCreateFile;
	String hostName = "68.181.174.149";
	int portNumber = 8111;

	protected void TestInterface() throws Exception {
		Scanner a = new Scanner(System.in);
		String input;
		do {
			System.out
					.print("Please Enter the Test you want to run (Enter X to exit)\n");
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
					if (tokens.length == 3) {
						test2(tokens[1], Integer.parseInt(tokens[2]));
					} else
						throw new Exception();
					break;
				case ("Test3"):
					if (tokens.length == 2)
						test3(tokens[1]);
					else
						throw new Exception();
					break;
				case ("Test4"):
					break;
				case ("Test5"):
					if (tokens.length == 3)
						test5(tokens[1].toString(), tokens[2].toString());
					else
						throw new Exception();
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
				System.err.print(e);
				System.out.println("Invalid OP or Parameters. \n");
			}
		} while (input != "X" || input != "x");

	}

	public void DealWithMessage(Message message) {
		if (message.type == msgType.DELETEDIRECTORY) {
			if (message.success == msgSuccess.REQUESTSUCCESS) {
				System.out.println("Deleted directory sucessfully!");
			} else {
				System.out.println("Error! Couldn't delete directory...");
			}
		}
		else if (message.type == msgType.CREATEDIRECTORY) {
			if(message.success == msgSuccess.REQUESTSUCCESS) {
				System.out.println("Successfully created directory "+message.filePath);
			}
			else {
				System.out.println("Failed to create directory");
			}
		}
		else if(message.type == msgType.READFILE)
		{
			// Supposedly going to cache it. Implementation will be completed
			// later.lol
			// uses the location to contact the chunkserver
			msgRequestAReadToChunkserver(message);
		} else if (message.type == msgType.PRINTFILEDATA) {
			msgPrintFileData(message);
		}
	}

	public void msgPrintFileData(Message dataMessage) {
		chunkReadsRecieved++;
		for (byte b : dataMessage.fileData)
			readFileData.add(b);
		System.out.print(dataMessage.fileData);
		if (chunkReadsRecieved == chunkCountToExpect) {
			System.out.print(dataMessage.fileData);
			byte[] finalByteArray = new byte[readFileData.size()];
			for (int n = 0; n < readFileData.size(); n++)
				finalByteArray[n] = readFileData.get(n);

			try {
				// convert array of bytes into file
				FileOutputStream fileOuputStream = new FileOutputStream(
						localPathToCreateFile);
				fileOuputStream.write(finalByteArray);
				fileOuputStream.close();

				System.out.println("Done");
			} catch (Exception e) {
				e.printStackTrace();
			}
			chunkCountToExpect = 99;
			chunkReadsRecieved = 0;
		}

	}

	public void msgRequestAReadToChunkserver(Message m) {

		chunkServer.DealWithMessage(m);
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
		// Example: Test2 1\2 3
		// Assuming the directory structure from the Test1 example above,
		// this Test would create 5 files in each directory 1\2, 1\2\4 and
		// 1\2\5.
		// The files in each directory would be named File1, File2, and File3.
		if (master.NamespaceMap.get(filepath) != null) {
			if (master.NamespaceMap.get(filepath).children.size() > 0) {
				List<String> childs = master.NamespaceMap.get(filepath).children;
				for (int a = 0; a < childs.size(); a++) {
					test2helper(childs.get(a), nFiles);
				}
			}
		} else
			System.out.println("Directory " + filepath + " not found");

		for (int i = 1; i <= nFiles; i++) {
			try {
				CCreateFile(filepath, (String) ("File" + i));
			} catch (Exception e) {
				System.out.println("Unable to create files");
			}
		}

		System.out.println(master.NamespaceMap.size());
		Iterator it = master.NamespaceMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry) it.next();
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
			it.remove(); // avoids a ConcurrentModificationException
		}

	}

	public void test2helper(String filepath, int nFiles) {

		String filename = "File";
		if (master.NamespaceMap.get(filepath) != null) {
			if (master.NamespaceMap.get(filepath).type != nodeType.FILE) {
				if (master.NamespaceMap.get(filepath).children.size() > 0) {
					List<String> childs = master.NamespaceMap.get(filepath).children;
					for (int a = 0; a < childs.size(); a++) {
						test2helper(childs.get(a), nFiles);
					}
				}
			} else
				System.out.println("Directory " + filepath + " not found");

			for (int i = 1; i <= nFiles; i++) {
				try {
					CCreateFile(filepath, (String) (filename + i));
				} catch (Exception e) {
					System.out.println("Unable to create files");
				}
			}
		}

	}

	public void CCreateFile(String folderFilepath, String fileName) {
		Message message = new Message(msgType.CREATEFILE, folderFilepath, 1);
		message.fileName = fileName;
		message.addressedTo = serverType.MASTER;
		message.sender = serverType.CLIENT;
		try {
			master.DealWithMessage(message);
		} catch (Exception e) {
			System.out.println("Unable to send message");
		}
	}

	public void test3(String filepath) {
		CDeleteDirectory(filepath);
	}

	public void CDeleteDirectory(String filepath) {
		// SENDING FILEPATH TO THE MASTER
		/*
		 * Properties prop = new Properties(); try { prop.load(new
		 * FileInputStream("config/config.properties")); } catch
		 * (FileNotFoundException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } catch (IOException e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); }
		 * System.out.println(prop.getProperty("IP1"));
		 * 
		 * try { Socket masterSocket = new Socket(prop.getProperty("IP1"),
		 * Integer.parseInt(prop.getProperty("PORT1"))); ObjectOutputStream out
		 * = new ObjectOutputStream(masterSocket.getOutputStream()); Message
		 * message = new Message(msgType.DELETEDIRECTORY);
		 * out.writeObject(message); out.close(); masterSocket.close(); } catch
		 * (NumberFormatException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } catch (UnknownHostException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); } catch (IOException
		 * e) { // TODO Auto-generated catch block e.printStackTrace(); }
		 */
		Message message = new Message(msgType.DELETEDIRECTORY);
		master.DealWithMessage(message);

	}

	/*
	 * public void test1(Integer numOfDir, String filepath) // recursively
	 * creates the specified num of // directories { Socket sock; try {
	 * Properties prop = new Properties(); prop.load(new
	 * FileInputStream("config/config.properties")); sock = new
	 * Socket(prop.getProperty("MASTERIP"),
	 * Integer.parseInt(prop.getProperty("MASTERPORT"))); ObjectOutputStream out
	 * = new ObjectOutputStream( sock.getOutputStream()); for (int i = 0; i <
	 * numOfDir; ++i) { Message message = new Message(Integer.toString(i + 1),
	 * msgType.CREATEDIRECTORY); out.writeObject(message); } // out.close(); //
	 * sock.close(); } catch (UnknownHostException e) { // TODO Auto-generated
	 * catch block e.printStackTrace(); } catch (IOException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); } }
	 */

	public void CCreateDirectory(String filepath) {
		Message message = new Message(msgType.CREATEDIRECTORY, filepath);
		message.sender = serverType.CLIENT;
		master.DealWithMessage(message);
	}

	public void test1(int NumFolders) {
		int count = 1;
		CCreateDirectory("1");
		if (NumFolders > 1) {
			helper("1", count * 2, NumFolders);
		}
		if (NumFolders > 2) {
			helper("1", count * 2 + 1, NumFolders);
		}
	}

	public void helper(String parentfilepath, int folderName, int NumMaxFolders) {
		if (folderName <= NumMaxFolders) {
			String newfilepath = parentfilepath + "\\" + folderName;
			CCreateDirectory(newfilepath);
			helper(newfilepath, folderName * 2, NumMaxFolders);
			helper(newfilepath, folderName * 2 + 1, NumMaxFolders);
		}

	}

	public void CCreateFile(String fullFilePath) { // including filename
		Message msg = new Message(fullFilePath, msgType.CREATEFILE);
		int index = fullFilePath.lastIndexOf('\\');
		msg.fileName = fullFilePath.substring(index + 1);
		msg.filePath = fullFilePath.substring(0, index);
		msg.addressedTo = serverType.MASTER;
		msg.sender = serverType.CLIENT;
		master.DealWithMessage(msg);

	}

	// Test 4 stores a file on the local machine in a target TFS specified by
	// its filepath
	public void test4(String localPath, String filePath) {
		// Step 1: Connect to the Master
		/*
		 * Plan: Send message to server including the filepath to createfile If
		 * no error returned, read the content of the local file and send to TFS
		 * Write to the created file If file >64MB (size of a chunk), write to
		 * several chunks
		 */

		CCreateFile(filePath); // empty file created

		FileInputStream fileInputStream = null;
		File localFile = new File(localPath);
		byte[] byteFile = new byte[(int) localFile.length()];

		// convert file into array of bytes
		try {
			fileInputStream = new FileInputStream(localFile);
			fileInputStream.read(byteFile);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		 * //now to cut it up into 64MB chunks if (byteFile.length >67108864){
		 * //67108864 bytes = 64MB int numChunks = ((byteFile.length -
		 * 1)/67108864) + 1; int currentIndex = 0; byte[][] Chunks = new
		 * byte[numChunks][]; //creates a list of byte arrays while(currentIndex
		 * <= numChunks-1){ Chunks[currentIndex] = Arrays.copyOf(byteFile,
		 * 67108864); //INCOMPLETE! NEED TO SEPARATE TO DIFFERENT CHUNKS } }
		 */

		// String masterIP = "68.181.174.149";
		// int masterPort = 8111;
		//
		// try {
		// Socket masterSocket = new Socket(masterIP, masterPort);
		// PrintWriter out = new PrintWriter(masterSocket.getOutputStream(),
		// true);
		// BufferedReader in = new BufferedReader(new
		// InputStreamReader(echoSocket.getInputStream()));
		// BufferedReader stdIn = new BufferedReader(new InputStreamReader(
		// System.in));
		//
		// } catch (UnknownHostException e) {
		// System.err.println("Don't know about host " + masterIP);
		// System.exit(1);
		// } catch (IOException e) {
		// e.printStackTrace();
		// System.err.println("Couldn't get I/O for the connection to "
		// + masterIP);
		// System.exit(1);
		// }
		// Step 2: Receive Message to be Written
		// if it exists, return error. else read content and store in TFS file
		// Pseudocode referring to
		// coderanch.com/t/205325/sockets/java/send-java-Object-socket
		// InputStream is = clientSocket.getInputStream();
		// ObjectInputStream ois = new ObjectInputStream(is);
		// receivedMsg rmsg = (Message)rmsg.readObject();
	}

	public void test5(String filePath, String localPath) {
		if (filePath == null || localPath == null) {
			System.out.println("Detected null values, please reenter query");
			return;
		}
		// Step 1 connect to the master
		// String masterIP = "68.181.174.149";
		// int masterPort = 8111;
		//
		// try {
		// Socket masterSocket = new Socket(masterIP, masterPort);
		// ObjectOutputStream objOut = new
		// ObjectOutputStream(masterSocket.getOutputStream());
		// // PrintWriter out = new PrintWriter(masterSocket.getOutputStream(),
		// true);
		// // BufferedReader in = new BufferedReader(new
		// InputStreamReader(echoSocket.getInputStream()));
		// BufferedReader stdIn = new BufferedReader( new
		// InputStreamReader(System.in));
		// //Step 2 Create a message
		// Message m = new Message(msgType.READFILE ,filePath);
		// //Step 3 Write to the master server
		// objOut.writeObject(m);
		// } catch (UnknownHostException e) {
		// System.err.println("Don't know about host " + masterIP);
		// System.exit(1);
		// } catch (IOException e) {
		// e.printStackTrace();
		// System.err.println("Couldn't get I/O for the connection to " +
		// masterIP);
		// System.exit(1);
		// }
		localPathToCreateFile = localPath;
		Message m = new Message(msgType.READFILE, filePath);
		m.sender = serverType.CLIENT;
		master.DealWithMessage(m);

		// Step 4 recieves the master message
		// Step 5 send a request to the chunkserver

	}

	public void ExpectChunkNumberForRead(int i) {
		chunkCountToExpect = i;
	}

}