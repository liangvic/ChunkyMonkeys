package Utility;

public class SOSMessage extends Message {
	public SOSMessage(String senderip, serverType senderType, int senderport,
			String receiverip, serverType receivertype, int receiverport)
	{
		super(senderip,senderType,senderport,
				receiverip, receivertype,receiverport);
		msgToServer = msgTypeToServer.TO_SOSSERVER;
		msgToMaster = msgTypeToMaster.REQUESTINGDATA;
	}
	public String SOSserver;
	public enum msgTypeToServer { TO_SOSSERVER, TO_OTHERSERVER, RECEIVINGDATA};
	public enum msgTypeToMaster { REQUESTINGDATA, DONESENDING };
	
	public msgTypeToServer msgToServer;
	public msgTypeToMaster msgToMaster;
}
