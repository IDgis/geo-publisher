package nl.idgis.publisher.database;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;

abstract class AbstractAutoCommit<T> extends UntypedActor {
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final ActorRef target;
	
	protected final T query;
	
	protected ActorRef transaction;
	
	protected AbstractAutoCommit(T query, ActorRef target) {
		this.query = query;
		this.target = target;
	}
	
	protected void completed() {
		
	}
	
	protected abstract void started();
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.apply(5, TimeUnit.MINUTES));
	}
	
	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof TransactionCreated) {
			log.debug("transaction created");
			
			transaction = ((TransactionCreated) msg).getActor();
			transaction.tell(query, getSelf());
			
			started();			
		} else if(msg instanceof Failure) {
			failure((Failure)msg);
		} else if(msg instanceof ReceiveTimeout) {
			timeout();
		} else {
			unhandled(msg);
		}
	}
	
	protected Procedure<Object> waitingForAck() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Failure) {
					failure((Failure)msg);
				} else if(msg instanceof Ack) {
					log.debug("transaction completed");
					
					completed();		
				
					getContext().stop(getSelf());
				} else if(msg instanceof ReceiveTimeout) {
					timeout();
				} else {
					unhandled(msg);
				}
			}
			
		};
	}

	protected void failure(Failure msg) {
		log.debug("failure");
		
		target.tell(msg, getContext().parent());
		
		getContext().stop(getSelf());
	}	
	
	protected void timeout() {
		log.error("timeout");

		getContext().stop(getSelf());
	}
}
