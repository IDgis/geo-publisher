package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.web.tree.GroupLayer;

public class GetGroupStructure implements DomainQuery<GroupLayer>{

	private static final long serialVersionUID = 3758325798457348393L;
	
	private final String groupId;
	
	public GetGroupStructure (final String groupId) {
		this.groupId = groupId;
	}
	
	public String groupId () {
		return this.groupId;
	}
	
}
 