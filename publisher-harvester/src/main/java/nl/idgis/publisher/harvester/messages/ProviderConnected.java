package nl.idgis.publisher.harvester.messages;

import java.io.Serializable;

public class ProviderConnected implements Serializable {	
	
	private static final long serialVersionUID = -6107698322151511761L;
	
	private final String name;
	
	public ProviderConnected(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "ProviderConnected [name=" + name + "]";
	}
}
