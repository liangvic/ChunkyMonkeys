package Utility;

import Utility.NamespaceNode.lockType;

public class lockInfo 
{
	public lockInfo(lockType type, int opID) {
		lockStatus = type;
		operationID = opID;
	}
	public lockType lockStatus = lockType.NONE;
	public int operationID = 0;
}