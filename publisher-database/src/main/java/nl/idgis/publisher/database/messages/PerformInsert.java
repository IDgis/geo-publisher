package nl.idgis.publisher.database.messages;

import com.mysema.query.sql.RelationalPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;
import com.mysema.query.types.SubQueryExpression;

public class PerformInsert extends Query {

	private static final long serialVersionUID = -5776721391154803535L;

	private final RelationalPath<?> entity;
	
    private final SubQueryExpression<?> subQuery;	
	
	private final Path<?>[] columns;

    private final Expression<?>[] values;
    
    public PerformInsert(RelationalPath<?> entity, SubQueryExpression<?> subQuery, Path<?>[] columns, Expression<?>[] values) {
    	this.entity = entity;
    	this.subQuery = subQuery;
    	this.columns = columns;
    	this.values = values;
    }

	public RelationalPath<?> getEntity() {
		return entity;
	}

	public SubQueryExpression<?> getSubQuery() {
		return subQuery;
	}

	public Path<?>[] getColumns() {
		return columns;
	}

	public Expression<?>[] getValues() {
		return values;
	}
}
