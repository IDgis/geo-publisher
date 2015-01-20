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
		CompletableFuture<T> future = new CompletableFuture<T>();
		
		transaction().whenComplete((tx, e0) -> {
			if(e0 == null) {			
				handler.apply(tx).whenComplete((t, e1) -> {
					log.debug("transaction handler completed");
					
					if(e1 != null) {
						tx.rollback().whenComplete((a, e2) -> {
							future.completeExceptionally(e1);
						});
					} else {
						tx.commit().whenComplete((a, e2) -> {
							if(e2 != null) {
								future.completeExceptionally(e2);
							} else {
								future.complete(t);
							}
						});
					}
				});
			} else {
				tx.rollback().whenComplete((a, e2) -> {
					future.completeExceptionally(e0);
				});
			}
		});
		
		return future;
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
