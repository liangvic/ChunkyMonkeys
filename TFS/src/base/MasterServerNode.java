package base;
import java.io.*;
import java.net.*;
import java.util.*;

public class MasterServerNode extends ServerNode{
	
	public static void main(String argv[]) throws Exception
    {
       String clientSentence;
       String capitalizedSentence;
       Properties prop = new Properties();
       prop.load(new FileInputStream("config/config.properties"));
       System.out.println(prop.getProperty("IP1"));
       ServerSocket welcomeSocket = new ServerSocket(6666);

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
       }
    }
	

	public class NamespaceNode{
		
	}
}
