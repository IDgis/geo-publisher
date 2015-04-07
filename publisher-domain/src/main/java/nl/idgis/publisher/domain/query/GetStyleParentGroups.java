package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.LayerGroup;

public class GetStyleParentGroups implements DomainQuery<Page<LayerGroup>> {
	
	private static final long serialVersionUID = -8479654177738576464L;
	
	private final String styleId;
	
	public GetStyleParentGroups (final String styleId) {
		this.styleId = styleId;
	}

	public String getId() {
		return styleId;
	}
	
}