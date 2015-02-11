package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;

public class IsFeatureTypeAvailable implements Serializable {

	private static final long serialVersionUID = 4704389553453680255L;
	
	private final String workspaceId, featureTypeId;
	
	public IsFeatureTypeAvailable(String workspaceId, String featureTypeId) {
		this.workspaceId = workspaceId;
		this.featureTypeId = featureTypeId;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public String getFeatureTypeId() {
		return featureTypeId;
	}

	@Override
	public String toString() {
		return "IsFeatureTypeAvailable [workspaceId=" + workspaceId
				+ ", featureTypeId=" + featureTypeId + "]";
	}
}
