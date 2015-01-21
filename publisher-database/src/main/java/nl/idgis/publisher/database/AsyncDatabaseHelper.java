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
		
		transaction().whenComplete((tx, t0) -> {
			if(t0 != null) {
				future.completeExceptionally(t0);
			} else {
				try {
					handler.apply(tx).whenComplete((t, t1) -> {
						log.debug("transaction handler completed");
						
						if(t1 != null) {
							log.error("future completed exceptionally: {}", t1);
							
							tx.rollback().whenComplete((a, t2) -> {
								if(t2 != null) {
									log.error("rollback failed: {}", t2);
								}
								
								future.completeExceptionally(t1);
							});
						} else {
							tx.commit().whenComplete((a, t2) -> {
								if(t2 != null) {
									log.error("commit failed: {}", t2);
									
									future.completeExceptionally(t2);
								} else {
									future.complete(t);
								}
							});
						}
					});
				} catch(Throwable t1) {
					log.error("transaction handler raised exception: {}", t1);
					
					tx.rollback().whenComplete((a, t2) -> {
						future.completeExceptionally(t1);
					});
				}
			}
		});
		
		return future;
	}
	
	public CompletableFuture<AsyncTransactionHelper> transaction() {
		CompletableFuture<AsyncTransactionHelper> future = new CompletableFuture<>();
		
		f.ask(actor, new StartTransaction())
			.whenComplete((msg, t) -> {
				if(t == null) {				
					if(msg instanceof TransactionCreated) {
						log.debug("transaction created");
					
						future.complete(new AsyncTransactionHelper(((TransactionCreated)msg).getActor(), f, log));
					} else {
						log.error("unknown message received: {}", msg);
						
						future.completeExceptionally(new IllegalArgumentException("TransactionCreated expected"));
					}
				} else {
					log.error("ask resulted in exception: {}", t);
					
					future.completeExceptionally(t);
				}
			});
		
		return future;
	}
}
