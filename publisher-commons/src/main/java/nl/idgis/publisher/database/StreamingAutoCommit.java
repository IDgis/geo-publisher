package nl.idgis.publisher.database;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Stop;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Terminated;
import akka.japi.Procedure;

public class StreamingAutoCommit extends AbstractAutoCommit<StreamingQuery> {
	
	public StreamingAutoCommit(StreamingQuery query, ActorRef target) {
		super(query, target);
	}
	
	public static Props props(StreamingQuery query, ActorRef target) {
		return Props.create(StreamingAutoCommit.class, query, target);
	}
	
	private Procedure<Object> waitingForStreamEnd() {
		return new Procedure<Object>() {
			
			ActorRef producer = transaction, consumer = target;

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Item) {
					log.debug("item");
					
					consume(msg);
				} else if(msg instanceof End) {
					log.debug("end");
					
					consume(msg);
					
					commit();
				} else if(msg instanceof NextItem) {
					log.debug("next item");
					
					produce(msg);
				} else if(msg instanceof Stop) {
					log.debug("stop");
					
					produce(msg);
					
					rollback();
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

			private void produce(Object msg) {
				log.debug("produce");
				
				consumer = getSender();
				
				producer.tell(msg, getSelf());
			}

			private void consume(Object msg) {
				log.debug("consume");
				
				producer = getSender();
				
				consumer.tell(msg, getSelf());
			}
			
			private void commit() {
				log.debug("requesting commit");
				
				transaction.tell(new Commit(), getSelf());
				
				getContext().become(waitingForAck());
			}
			
			private void rollback() {
				log.debug("requesting rollback");
				
				transaction.tell(new Rollback(), getSelf());
				
				getContext().become(waitingForAck());
			}
			
		};
	}

	@Override
	protected void onStarted() {
		getContext().become(waitingForStreamEnd());
	}
	
}
