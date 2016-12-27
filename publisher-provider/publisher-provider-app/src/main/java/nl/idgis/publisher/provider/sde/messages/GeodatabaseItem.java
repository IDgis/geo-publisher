package nl.idgis.publisher.provider.sde.messages;

import java.io.Serializable;

public class GeodatabaseItem implements Serializable {

	private static final long serialVersionUID = -4083049150450916793L;

	private final String uuid;
	
	private final String physicalname;
	
	private final String definition;
	
	private final String documentation;
	
	public GeodatabaseItem(String uuid, String physicalname, String definition, String documentation) {
		this.uuid = uuid;
		this.physicalname = physicalname;
		this.definition = definition;
		this.documentation = documentation;
	}

	public String getUuid() {
		return uuid;
	}

	public String getPath() {
		return physicalname;
	}

	public String getDefinition() {
		return definition;
	}

	public String getDocumentation() {
		return documentation;
	}

	@Override
	public String toString() {
		return "GeodatabaseItem [uuid=" + uuid + ", physicalname=" + physicalname + "]";
	}
}
