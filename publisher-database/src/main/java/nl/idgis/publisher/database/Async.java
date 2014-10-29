package nl.idgis.publisher.database;

import nl.idgis.publisher.utils.TypedList;

import scala.concurrent.Future;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;

public interface Async {

	Future<TypedList<Tuple>> list(Expression<?>... args);
	<RT> Future<TypedList<RT>> list(Expression<RT> projection);
	Future<TypedList<Tuple>> list(Object... args);
}
