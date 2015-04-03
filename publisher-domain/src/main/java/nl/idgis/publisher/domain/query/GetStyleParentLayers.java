package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.web.Layer;

public class GetStyleParentLayers implements DomainQuery<Page<Layer>> {
	
	private static final long serialVersionUID = 1656285331408494982L;
	
	private final String styleId;
	
	public GetStyleParentLayers (final String styleId) {
		this.styleId = styleId;
	}

	public String getId() {
		return styleId;
	}
	
}