package Utility;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 *
 */
public class TFSLogger {
	public final static Logger tfsLogger = Logger.getLogger(TFSLogger.class.getName());
	private static FileHandler fh;
	
	public static void LogMsg(String logMsg) 
	{
		try {
			fh = new FileHandler("logger.log", true);
			tfsLogger.addHandler(fh);
			tfsLogger.setLevel(Level.FINE);
			SimpleFormatter formatter = new SimpleFormatter();  
		    fh.setFormatter(formatter); 
			tfsLogger.fine(logMsg);
			fh.flush();
			fh.close();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
