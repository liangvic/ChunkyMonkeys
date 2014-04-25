package Utility;

import java.util.ArrayList;
import java.util.List;

import Utility.NamespaceNode.lockType;

/**
 * 
 *
 */
public class NamespaceNode {
	public static enum nodeType{ DIRECTORY, FILE}
	public nodeType type;
	public List<String> children = new ArrayList<String>();
	public NamespaceNode(nodeType t){
		type = t;
	}
	//file access permissions?
	public static enum lockType { NONE, SHARED, I_SHARED, EXCLUSIVE, I_EXCLUSIVE }
	//public lockInfo lockData;
	public List<lockInfo> lockList = new ArrayList<lockInfo>();
	
}
