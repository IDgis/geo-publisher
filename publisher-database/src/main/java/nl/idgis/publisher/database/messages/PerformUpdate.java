package nl.idgis.publisher.database.messages;

import java.util.List;

import com.mysema.query.QueryMetadata;
import com.mysema.query.sql.RelationalPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;

public class PerformUpdate extends Query {	

	private static final long serialVersionUID = 4017014096238120583L;

	private final RelationalPath<?> entity;
	
	private final List<Path<?>> columns;

    private final List<Expression<?>> values;
    
    private final QueryMetadata metadata;
	
	public PerformUpdate(RelationalPath<?> entity, List<Path<?>> columns, List<Expression<?>> values, QueryMetadata metadata) {
		this.entity = entity;
		this.columns = columns;
		this.values = values;
		this.metadata = metadata;		
	}

	public RelationalPath<?> getEntity() {
		return entity;
	}

	public List<Path<?>> getColumns() {
		return columns;
	}

	public List<Expression<?>> getValues() {
		return values;
	}

	public QueryMetadata getMetadata() {
		return metadata;
	}	

}
