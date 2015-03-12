package nl.idgis.publisher.database;

import java.util.Optional;
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
		
		f.ask(actorRef, new StartTransaction())
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

	@Override
	public AsyncTransactionRef getTransactionRef() {
		throw new IllegalStateException("not a transaction");
	}
	
	private AsyncHelper bind(AsyncTransactionRef transactionRef) {
		return new AsyncTransactionHelper(transactionRef.getActorRef(), f, log);
	}
	
	public <T> CompletableFuture<T> transactional(Optional<AsyncTransactionRef> transactionRef, Function<AsyncHelper, CompletableFuture<T>> func) {
		if(transactionRef.isPresent()) {
			return func.apply(bind(transactionRef.get()));
		} else {
			return transactional(tx -> func.apply(tx));
		}
	}
	
	public <T extends AsyncTransactional, U> CompletableFuture<U> transactional(T msg, Function<AsyncHelper, CompletableFuture<U>> func) {
		return transactional(msg.getTransactionRef(), func);
	}
}
