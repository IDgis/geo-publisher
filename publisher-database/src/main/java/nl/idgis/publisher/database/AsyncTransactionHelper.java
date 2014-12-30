package nl.idgis.publisher.database;

import scala.concurrent.ExecutionContext;

import akka.actor.ActorRef;
import akka.dispatch.Mapper;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;
import akka.util.Timeout;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.SmartFuture;

public final class AsyncTransactionHelper extends AbstractAsyncHelper {  
	
	AsyncTransactionHelper(ActorRef transaction, Timeout timeout, ExecutionContext executionContext, LoggingAdapter log) {		
		super(transaction, timeout, executionContext, log);
	}
	
	public SmartFuture<Ack> commit() {
		return new SmartFuture<>(Patterns.ask(actor, new Commit(), timeout).map(new Mapper<Object, Ack>() {
			
			@Override
			public Ack checkedApply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("committed");
					
					return (Ack)msg;
				} else {
					log.debug("commit failed");
					
					throw new IllegalStateException("commit failed");
				}
			}
			
		}, executionContext), executionContext);
	}
	
	public SmartFuture<Ack> rollback() {
		return new SmartFuture<>(Patterns.ask(actor, new Rollback(), timeout).map(new Mapper<Object, Ack>() {
			
			@Override
			public Ack checkedApply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("rolled back");
					
					return (Ack)msg;
				} else {
					log.debug("rollback failed");
					
					throw new IllegalStateException("rollback failed");
				}
			}
			
		}, executionContext), executionContext);
	}
}
