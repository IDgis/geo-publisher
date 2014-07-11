package nl.idgis.publisher.monitor.messages;

import java.io.Serializable;

public class Tree implements Serializable {
	
	private static final long serialVersionUID = -1399058412949973059L;
	
	private final String name;
	private final Node[] children;

	public Tree(String name, Node[] children) {
		this.name = name;
		this.children = children;
	}
	
	public String getName() {
		return name;
	}
	
	public Node[] getChildren() {
		return children;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Tree: ");
		sb.append(name);
		sb.append("\n");
				
		String separator = "";		
		for(Node child : children) {
			sb.append(separator);
			sb.append(child.toStringBuilder(1));
			separator = "\n";
		}
		
		return sb.toString();
	}
}
