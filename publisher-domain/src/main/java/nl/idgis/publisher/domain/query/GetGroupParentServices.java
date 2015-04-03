package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Service;

public class GetGroupParentServices implements DomainQuery<Page<Service>> {
	
	private static final long serialVersionUID = 1126716004751273269L;
	
	private final String groupId;
	
	public GetGroupParentServices (final String groupId) {
		this.groupId = groupId;
	}

	public String getId() {
		return groupId;
	}
	
}