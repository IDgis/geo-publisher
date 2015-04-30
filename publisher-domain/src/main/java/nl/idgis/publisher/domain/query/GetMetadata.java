package nl.idgis.publisher.domain.query;

import nl.idgis.publisher.domain.web.Metadata;

public class GetMetadata implements DomainQuery<Metadata> {
	
	private static final long serialVersionUID = -7624869095636704338L;

	private final String id;
	
	private final String stylesheet;
	
	public GetMetadata(String id) {
		this(id, null);
	}
	
	public GetMetadata(String id, String stylesheet) {
		this.id = id;
		this.stylesheet = stylesheet;
	}

	public String id() {
		return id;
	}

	public String stylesheet() {
		return stylesheet;
	}

	@Override
	public String toString() {
		return "GetMetadata [id=" + id + ", stylesheet=" + stylesheet + "]";
	}
}
