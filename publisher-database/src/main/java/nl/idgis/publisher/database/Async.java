package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.utils.TypedList;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;

public interface Async {

	CompletableFuture<TypedList<Tuple>> list(Expression<?>... args);
	<RT> CompletableFuture<TypedList<RT>> list(Expression<RT> projection);
	CompletableFuture<TypedList<Tuple>> list(Object... args);
	CompletableFuture<Tuple> singleResult(Expression<?>... args);
	<RT> CompletableFuture<RT> singleResult(Expression<RT> projection);
	CompletableFuture<Tuple> singleResult(Object... args);
	CompletableFuture<Boolean> exists();
	CompletableFuture<Boolean> notExists();
}
