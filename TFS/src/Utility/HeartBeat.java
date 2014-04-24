package Utility;

import Utility.Message.serverType;

/**
 * Message subclass between servers and Master for heartbeat implementation
 *
 */
public class HeartBeat extends Message{
	public HeartBeat(String senderip, serverType sendertype, int senderport, 
			String recieverip, serverType receivertype,int receiverport, serverStatus nserverStatus) {
		super(senderip, sendertype, senderport, 
				recieverip, receivertype,receiverport);
		status = nserverStatus;
	}
	public static enum serverStatus {ALIVE, DEAD, OUTDATED};
	public serverStatus status;	
}
