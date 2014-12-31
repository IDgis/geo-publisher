package nl.idgis.publisher.database;

import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.function.Function1;
import nl.idgis.publisher.utils.SmartFuture;

import scala.concurrent.ExecutionContext;

import akka.actor.ActorRef;
import akka.dispatch.Mapper;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class AsyncDatabaseHelper extends AbstractAsyncHelper {
	
	public AsyncDatabaseHelper(ActorRef database, Timeout timeout, ExecutionContext executionContext, LoggingAdapter log) {
		super(database, timeout, executionContext, log);
	}
	
	public <T> SmartFuture<T> transactional(final Function1<AsyncHelper, SmartFuture<T>> handler) {
		return transaction().flatMap(tx -> {
				try {
					return handler.apply(tx).flatMap(t -> {
						return tx.commit().map(msg -> t);
					});
				} catch(final Exception e) {
					return tx.rollback().map(msg -> {
						throw e;
					});
				}
			});
	}
	
	public SmartFuture<AsyncTransactionHelper> transaction() {
		return new SmartFuture<>(Patterns.ask(actor, new StartTransaction(), timeout)
			.map(new Mapper<Object, AsyncTransactionHelper>() {
				
				@Override
				public AsyncTransactionHelper checkedApply(Object msg) throws Exception {
					if(msg instanceof TransactionCreated) {
						log.debug("transaction created");
					
						return new AsyncTransactionHelper(((TransactionCreated)msg).getActor(), timeout, executionContext, log);
					} else {
						throw new IllegalArgumentException("TransactionCreated expected");
					}
				}
			}, executionContext), executionContext);
	}
}
