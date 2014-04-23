package Utility;

/**
 * Message subclass between servers and Master for heartbeat implementation
 *
 */
public class HeartBeat extends Message{
	public HeartBeat(String nsenderIP, serverType nsenderType, serverStatus nserverStatus) {
		super();
		senderIP = nsenderIP;
		sender = nsenderType;
		status = nserverStatus;
	}
	public static enum serverStatus {ALIVE, DEAD, OUTDATED};
	public serverStatus status;	
}
