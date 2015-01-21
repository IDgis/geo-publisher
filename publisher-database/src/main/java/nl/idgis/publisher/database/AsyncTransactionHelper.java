package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

public final class AsyncTransactionHelper extends AbstractAsyncHelper {  
	
	AsyncTransactionHelper(ActorRef transaction, FutureUtils f, LoggingAdapter log) {		
		super(transaction, f, log);
	}
	
	public CompletableFuture<Ack> commit() {
		return f.ask(actor, new Commit())
			.thenApply(msg -> {
				if(msg instanceof Ack) {
					log.debug("committed");
					
					return (Ack)msg;
				} else if(msg instanceof Failure) {
					log.error("commit failed: {}", msg);
					
					throw new IllegalStateException("commit failed", ((Failure) msg).getCause());
				} else {
					log.error("commit failed");
					
					throw new IllegalStateException("commit failed");
				}
			});
	}
	
	public CompletableFuture<Ack> rollback() {
		return f.ask(actor, new Rollback())
			.thenApply(msg -> {
				if(msg instanceof Ack) {
					log.debug("rolled back");
					
					return (Ack)msg;
				} else if(msg instanceof Failure) {
					log.error("rollback failed: {}", msg);
					
					throw new IllegalStateException("rollback failed", ((Failure) msg).getCause());
				} else {
					log.error("rollback failed");
					
					throw new IllegalStateException("rollback failed");
				}
			});
	}
}
