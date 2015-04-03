package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.LayerGroup;

public class GetGroupParentGroups implements DomainQuery<Page<LayerGroup>> {
	
	private static final long serialVersionUID = -3631070907353705131L;
	
	private final String groupId;
	
	public GetGroupParentGroups (final String groupId) {
		this.groupId = groupId;
	}

	public String getId() {
		return groupId;
	}
	
}