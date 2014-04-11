package Utility;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class TFSLogger {
	private final static Logger tfsLogger = Logger.getLogger(TFSLogger.class.getName());
	private FileHandler fh;
	
	public TFSLogger() 
	{
		try {
			fh = new FileHandler("logger.log", true);
			tfsLogger.addHandler(fh);
			tfsLogger.setLevel(Level.FINE);
			SimpleFormatter formatter = new SimpleFormatter();  
		    fh.setFormatter(formatter); 
			
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void LogMsg(String logMsg) 
	{
		tfsLogger.fine(logMsg);
		fh.flush();
		//fh.close();
	}
}
