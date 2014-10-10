package nl.idgis.publisher.database;

import nl.idgis.publisher.utils.TypedIterable;

import scala.concurrent.Future;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;

public interface Async {

	Future<TypedIterable<Tuple>> list(Expression<?>... args);
	<RT> Future<TypedIterable<RT>> list(Expression<RT> projection);
	Future<TypedIterable<Tuple>> list(Object... args);
}
