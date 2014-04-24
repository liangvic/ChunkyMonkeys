package Utility;

public class SOSMessage extends Message {
	public SOSMessage(String senderip, serverType senderType, int senderport,
			String receiverip, serverType receivertype, int receiverport)
	{
		super(senderip,senderType,senderport,
				receiverip, receivertype,receiverport);
	}
	public String SOSserver;
}
