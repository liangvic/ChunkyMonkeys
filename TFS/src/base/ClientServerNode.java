package base;

import java.io.*;
import java.net.*;
import java.util.*;

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
			System.err.println("Couldn't get I/O for the connection to "
					+ hostName);
			System.exit(1);
		}
	}
}