package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;

public class EnsureWorkspace implements Serializable {

	private static final long serialVersionUID = 4420711244564099791L;
	
	private final String workspaceId;
	
	public EnsureWorkspace(String workspaceId) {
		this.workspaceId = workspaceId;
	}
	
	public String getWorkspaceId() {
		return workspaceId;
	}

	@Override
	public String toString() {
		return "EnsureWorkspace [workspaceId=" + workspaceId + "]";
	}
}
