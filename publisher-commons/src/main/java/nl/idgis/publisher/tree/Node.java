package nl.idgis.publisher.tree;

import java.io.Serializable;

public abstract class Node implements Serializable {

	private static final long serialVersionUID = -6477411997768999518L;
	
	protected final String name;
	
	Node(String name) {
		this.name = name;	
	}
	
	public String getName() {
		return name;
	}
	
	abstract StringBuilder toStringBuilder(int depth);
	
	@Override
	public final String toString() {
		return toStringBuilder(0).toString();
	}
}
