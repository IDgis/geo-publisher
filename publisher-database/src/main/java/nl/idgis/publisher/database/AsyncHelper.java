package nl.idgis.publisher.database;

import com.mysema.query.sql.RelationalPath;

import scala.concurrent.Future;

import nl.idgis.publisher.utils.FutureUtils.Collector1;

public interface AsyncHelper {

	AsyncSQLQuery query();
	
	AsyncSQLInsertClause insert(RelationalPath<?> entity);

	<T> Collector1<T> collect(Future<T> future);
}
