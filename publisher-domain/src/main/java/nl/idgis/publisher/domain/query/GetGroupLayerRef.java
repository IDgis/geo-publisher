package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.web.tree.LayerRef;

public class GetGroupLayerRef implements DomainQuery<LayerRef<?>> {	
	
	private static final long serialVersionUID = -962486985419801276L;
	
	private final String groupId;

	public GetGroupLayerRef(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupId() {
		return groupId;
	}

	@Override
	public String toString() {
		return "GetGroupLayerRef [groupId=" + groupId + "]";
	}

}
