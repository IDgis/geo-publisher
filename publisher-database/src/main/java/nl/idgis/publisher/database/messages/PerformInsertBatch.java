package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;
import com.mysema.query.types.SubQueryExpression;

public class PerformInsertBatch implements Serializable {
	
	private static final long serialVersionUID = -2600866780455024897L;

	private final Path<?>[] columns;

	private final Expression<?>[] values;
	 
	private final SubQueryExpression<?> subQuery;
	 
	public PerformInsertBatch(Path<?>[] columns, Expression<?>[] values, Optional<SubQueryExpression<?>> optionalSubQuery) {
		Objects.requireNonNull(columns);
		Objects.requireNonNull(values);
		
		int valueCount = 
			optionalSubQuery
				.map(subQuery -> subQuery.getMetadata().getProjection().size())
				.orElse(values.length);
		
		if(columns.length != valueCount) {
			throw new IllegalArgumentException("columns length (" + columns.length + ") and values length (" + valueCount + ") mismatch");
		}
		
		this.columns = columns;
		this.values = values;
		this.subQuery = optionalSubQuery.orElse(null);
	}

	public Path<?>[] getColumns() {
		return columns;
	}

	public Expression<?>[] getValues() {
		return values;
	}

	public Optional<SubQueryExpression<?>> getSubQuery() {
		return Optional.ofNullable(subQuery);
	}
	
	@Override
	public String toString() {
		return "PerformInsertBatch [columns=" + columns + ", values=" + values
				+ ", subQuery=" + subQuery + "]";
	}
}
