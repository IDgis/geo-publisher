package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import akka.actor.ActorRef;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.utils.FutureUtils;

public class AsyncDatabaseHelper extends AbstractAsyncHelper {
	
	public AsyncDatabaseHelper(ActorRef database, FutureUtils f, LoggingAdapter log) {
		super(database, f, log);
	}
	
	public <T> CompletableFuture<T> transactional(final Function<AsyncHelper, CompletableFuture<T>> handler) {
		return transaction().thenCompose(tx -> {
				try {
					return handler.apply(tx).thenCompose(t -> {
						return tx.commit().thenApply(msg -> t);
					});
				} catch(final Exception e) {
					return tx.rollback().thenApply(msg -> {
						throw e;
					});
				}
			});
	}
	
	public CompletableFuture<AsyncTransactionHelper> transaction() {
		return f.ask(actor, new StartTransaction())
			.thenApply(msg -> {
					if(msg instanceof TransactionCreated) {
						log.debug("transaction created");
					
						return new AsyncTransactionHelper(((TransactionCreated)msg).getActor(), f, log);
					} else {
						throw new IllegalArgumentException("TransactionCreated expected");
					}
			});
	}
}
