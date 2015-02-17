package nl.idgis.publisher.service.geoserver.messages;

import java.io.Serializable;
import java.util.List;

public class EnsureWorkspace implements Serializable {

	private static final long serialVersionUID = -2616789799155772596L;

	private final String workspaceId, title, abstr;
	
	private final List<String> keywords;
	
	public EnsureWorkspace(String workspaceId, String title, String abstr, List<String> keywords) {
		this.workspaceId = workspaceId;
		this.title = title;
		this.abstr = abstr;
		this.keywords = keywords;
	}
	
	public String getWorkspaceId() {
		return workspaceId;
	}

	public String getTitle() {
		return title;
	}

	public String getAbstract() {
		return abstr;
	}

	public List<String> getKeywords() {
		return keywords;
	}

	@Override
	public String toString() {
		return "EnsureWorkspace [workspaceId=" + workspaceId + "]";
	}
}
