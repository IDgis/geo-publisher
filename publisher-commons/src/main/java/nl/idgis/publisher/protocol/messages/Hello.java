package nl.idgis.publisher.protocol.messages;

import java.io.Serializable;

public class Hello implements Serializable {
	
	private static final long serialVersionUID = 6884946820561493766L;
	
	private final String name;
	
	public Hello(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Hello [name=" + name + "]";
	}
}
