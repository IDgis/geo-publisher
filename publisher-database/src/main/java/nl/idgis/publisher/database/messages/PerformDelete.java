package nl.idgis.publisher.database.messages;

import com.mysema.query.DefaultQueryMetadata;
import com.mysema.query.sql.RelationalPath;

public class PerformDelete extends Query {
	
	private static final long serialVersionUID = 4597354997277162900L;

	private final RelationalPath<?> entity;
	
	private final DefaultQueryMetadata metadata;

	public PerformDelete(RelationalPath<?> entity, DefaultQueryMetadata metadata) {
		this.entity = entity;
		this.metadata = metadata;
	}

	public RelationalPath<?> getEntity() {
		return entity;
	}

	public DefaultQueryMetadata getMetadata() {
		return metadata;
	}

	@Override
	public String toString() {
		return "PerformDelete [entity=" + entity + ", metadata=" + metadata
				+ "]";
	}
	
}
