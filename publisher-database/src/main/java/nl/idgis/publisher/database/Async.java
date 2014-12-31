package nl.idgis.publisher.database;

import nl.idgis.publisher.utils.SmartFuture;
import nl.idgis.publisher.utils.TypedList;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;

public interface Async {

	SmartFuture<TypedList<Tuple>> list(Expression<?>... args);
	<RT> SmartFuture<TypedList<RT>> list(Expression<RT> projection);
	SmartFuture<TypedList<Tuple>> list(Object... args);
	SmartFuture<Tuple> singleResult(Expression<?>... args);
	<RT> SmartFuture<RT> singleResult(Expression<RT> projection);
	SmartFuture<Tuple> singleResult(Object... args);
	SmartFuture<Boolean> exists();
	SmartFuture<Boolean> notExists();
}
