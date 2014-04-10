import java.io.FileInputStream;
import java.util.Properties;


public class MasterServerNode extends ServerNode{
	
protected:
	
		
	public static void main(String argv[]) throws Exception
    {
       String clientSentence;
       String capitalizedSentence;
       Properties prop = new Properties();
       prop.load(new FileInputStream("config/config.properties"));
       //use prop.getProperty("IP1") etc to get values from config file
       ServerSocket welcomeSocket = new ServerSocket(6789);

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
