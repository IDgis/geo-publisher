package nl.idgis.publisher.monitor.messages;

public abstract class Node {

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
