package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import nl.idgis.publisher.protocol.messages.Ack;

public interface Transaction<T> {
	
	<U> CompletableFuture<U> apply(Function<T, CompletableFuture<U>> handler);
	
	CompletableFuture<Ack> commit();
	CompletableFuture<Ack> rollback();
}