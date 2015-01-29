package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import akka.actor.ActorRef;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.utils.FutureUtils;

public class AsyncDatabaseHelper extends AbstractAsyncHelper implements TransactionSupplier<AsyncHelper> {
	
	private final TransactionHandler<AsyncHelper> transactionHandler;
	
	public AsyncDatabaseHelper(ActorRef database, FutureUtils f, LoggingAdapter log) {
		super(database, f, log);
		
		transactionHandler = new TransactionHandler<AsyncHelper>(this, log);
	}
	
	public <T> CompletableFuture<T> transactional(final Function<AsyncHelper, CompletableFuture<T>> handler) {		
		return transactionHandler.transactional(handler);
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
