package nl.idgis.publisher.database;

import com.mysema.query.sql.RelationalPath;

public interface AsyncHelper {

	AsyncSQLQuery query();
	
	AsyncSQLInsertClause insert(RelationalPath<?> entity);
	
	AsyncSQLUpdateClause update(RelationalPath<?> entity);	
}
