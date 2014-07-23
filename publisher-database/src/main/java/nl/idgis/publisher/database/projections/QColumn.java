package nl.idgis.publisher.database.projections;

import nl.idgis.publisher.domain.service.Column;

import com.mysema.query.types.ConstructorExpression;
import com.mysema.query.types.Expression;

public class QColumn extends ConstructorExpression<Column>{

	private static final long serialVersionUID = 5555217006415319690L;

	public QColumn(Expression<String> name, Expression<String> dataType) {
		super(Column.class, new Class<?>[]{String.class, String.class}, name, dataType);
	}
}
