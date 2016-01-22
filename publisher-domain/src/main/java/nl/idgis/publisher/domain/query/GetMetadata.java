package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.web.Metadata;

public class GetMetadata implements DomainQuery<Metadata> {
	
	private static final long serialVersionUID = -7624869095636704338L;

	private final String id;
	
	public GetMetadata(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	@Override
	public String toString() {
		return "GetMetadata [id=" + id + "]";
	}
}
