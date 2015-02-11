package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;

public class EnsureGroup implements Serializable {

	private static final long serialVersionUID = -1283470898563050308L;
	
	private final String groupId;
	
	public EnsureGroup(String groupId) {
		this.groupId = groupId;
	}
	
	public String getGroupId() {
		return groupId;
	}

	@Override
	public String toString() {
		return "EnsureGroup [groupId=" + groupId + "]";
	}
}
