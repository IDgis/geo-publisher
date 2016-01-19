package model.dav;

public class DefaultResourceProperties implements ResourceProperties {
	
	private final boolean collection;
	
	public DefaultResourceProperties(boolean collection) {
		this.collection = collection;
	}

	public boolean collection() {
		return collection;
	}
}