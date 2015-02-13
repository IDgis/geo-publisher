package nl.idgis.publisher.service.manager.messages;

import java.io.Serializable;

public class GetGroupLayer implements Serializable {

	private static final long serialVersionUID = -7632901171842829234L;
	
	private final String groupLayerId;

	public GetGroupLayer(String groupLayerId) {
		this.groupLayerId = groupLayerId;
	}
	
	public String getGroupLayerId() {
		return groupLayerId;
	}

	@Override
	public String toString() {
		return "GetGroupLayer [groupLayerId=" + groupLayerId + "]";
	}
	
}
