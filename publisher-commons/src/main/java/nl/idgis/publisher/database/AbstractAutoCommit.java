package nl.idgis.publisher.database;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.database.messages.Rollback;
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
	
	protected void onCompleted() {
		
	}
	
	protected abstract void onStarted();
	
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
			
			getContext().watch(transaction);
			
			onStarted();			
		} else if(msg instanceof Failure) {
			handleFailure((Failure)msg);
		} else if(msg instanceof ReceiveTimeout) {
			handleTimeout();
		} else if(msg instanceof Terminated) {
			handleTerminated((Terminated)msg);
		} else {
			unhandled(msg);
		}
	}
	
	protected void handleTerminated(Terminated msg) {
		ActorRef actor = msg.getActor();
		if(actor.equals(transaction)) {
			log.warning("transaction terminated");
			
			getContext().stop(getSelf());
		} else {
			log.error("unknown actor terminated");
		}
	}

	protected Procedure<Object> waitingForAck() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Failure) {
					handleFailure((Failure)msg);
				} else if(msg instanceof Ack) {
					log.debug("transaction completed");
					
					onCompleted();		
				
					getContext().stop(getSelf());
				} else if(msg instanceof ReceiveTimeout) {
					handleTimeout();
				} else if(msg instanceof Terminated) {
					handleTerminated((Terminated)msg);
				} else {
					unhandled(msg);
				}
			}
			
		};
	}

	protected void handleFailure(Failure msg) {
		log.debug("failure");
		
		target.tell(msg, getContext().parent());
		
		getSender().tell(new Rollback(), getSelf());
		
		getContext().become(waitingForAck());
	}	
	
	protected void handleTimeout() {
		log.error("timeout");

		getContext().stop(getSelf());
	}
}
