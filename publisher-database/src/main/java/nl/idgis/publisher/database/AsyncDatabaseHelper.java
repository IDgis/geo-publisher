package nl.idgis.publisher.database;

import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Ack;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.dispatch.Mapper;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class AsyncDatabaseHelper extends AbstractAsyncHelper {
	
	public AsyncDatabaseHelper(ActorRef database, Timeout timeout, ExecutionContext executionContext, LoggingAdapter log) {
		super(database, timeout, executionContext, log);
	}
	
	public <T> Future<T> transactional(final Function<AsyncHelper, Future<T>> handler) {
		return transaction().flatMap(new Mapper<AsyncTransactionHelper, Future<T>>() {
			
			public Future<T> checkedApply(final AsyncTransactionHelper tx) throws Exception {
				try {
					return handler.apply(tx).flatMap(new Mapper<T, Future<T>>() {
						
						public Future<T> checkedApply(final T t) throws Exception {
							return tx.commit().map(new Mapper<Ack, T>() {
								
								@Override
								public T apply(Ack msg) {
									return t;
								}
								
							}, executionContext);
						}
						
					}, executionContext);
				} catch(final Exception e) {
					return tx.rollback().map(new Mapper<Ack, T>() {
						
						@Override
						public T checkedApply(Ack msg) throws Exception {
							throw e;
						}
						
					}, executionContext);
				}
			}
			
		}, executionContext);
	}
	
	public Future<AsyncTransactionHelper> transaction() {
		return Patterns.ask(actor, new StartTransaction(), timeout)
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
			}, executionContext);
	}
}
