package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;

public class IsWorkspaceAvailable implements Serializable {

	private static final long serialVersionUID = 4420711244564099791L;
	
	private final String workspaceId;
	
	public IsWorkspaceAvailable(String workspaceId) {
		this.workspaceId = workspaceId;
	}
	
	public String getId() {
		return workspaceId;
	}

	@Override
	public String toString() {
		return "IsWorkspaceAvailable [workspaceId=" + workspaceId + "]";
	}
}
