package nl.idgis.publisher.database.messages;

import com.mysema.query.QueryMetadata;

public class PerformQuery extends Query {
	
	private static final long serialVersionUID = 6288816529749473772L;
	
	private final QueryMetadata metadata;

	public PerformQuery(QueryMetadata metadata) {
		this.metadata = metadata;
	}
	
	public QueryMetadata getMetadata() {
		return metadata;
	}
}
