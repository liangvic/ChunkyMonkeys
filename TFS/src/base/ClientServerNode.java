package base;

import java.io.*;
import java.net.*;
import java.util.*;

import base.ServerNode;

public class ClientServerNode extends ServerNode {
	public static void main(String args[]) throws Exception {

		String hostName = "68.181.174.67";
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
		try (
			Socket masterSocket = new Socket(prop.getProperty("IP1"), Integer.parseInt(prop.getProperty("PORT1")));
			//code from above to talk to the Master
			//ONLY CHANGE THE WHILE LOOP
			PrintWriter out =
					new PrintWriter(masterSocket.getOutputStream(), true);
			BufferedReader in =
					new BufferedReader(
							new InputStreamReader(masterSocket.getInputStream()));
			BufferedReader stdIn =
					new BufferedReader(
							new InputStreamReader(System.in))
			) {
		String userInput;
		while ((userInput = stdIn.readLine()) != null) {
			out.println(userInput);
			System.out.println(filepath); //ONLY CHANGED THIS
		}} catch (NumberFormatException e) {
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
}