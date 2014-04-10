package base;
import java.io.*;
import java.net.*;
import java.util.*;

public class MasterServerNode extends ServerNode{
	
	//private static ServerSocket welcomeSocket;


	public static void main(String args[]) throws Exception
    {
	        int portNumber = 8111;
	        
	        try (
	            ServerSocket serverSocket =
	                new ServerSocket(portNumber);
	            Socket clientSocket = serverSocket.accept();     
	            PrintWriter out =
	                new PrintWriter(clientSocket.getOutputStream(), true);                   
	            BufferedReader in = new BufferedReader(
	                new InputStreamReader(clientSocket.getInputStream()));
	        ) {
	            String inputLine;
	            while ((inputLine = in.readLine()) != null) {
	                //out.println(inputLine);
	            	DealWithMessage(inputLine); //separate message to deal with input
	            }
	        } catch (IOException e) {
	            System.out.println("Exception caught when trying to listen on port "
	                + portNumber + " or listening for a connection");
	            System.out.println(e.getMessage());
	        }
       /*String clientSentence;
       String capitalizedSentence;
       Properties prop = new Properties();
       prop.load(new FileInputStream("config/config.properties"));
       System.out.println(prop.getProperty("IP1"));
       welcomeSocket = new ServerSocket(9090);
       
       while(true)
       {
          Socket connectionSocket = welcomeSocket.accept();
          BufferedReader inFromClient =
             new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
          DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
          clientSentence = inFromClient.readLine();
          System.out.println("Received: " + clientSentence);
          capitalizedSentence = clientSentence.toUpperCase() + '\n';
          outToClient.writeBytes(capitalizedSentence);
       }*/
    }
	

	public class NamespaceNode{
		
	}
	
	public static void DealWithMessage(String line)
	{
		
	}
	
	public void MDeleteDirectory()
	{
		
	}
	
	
}
