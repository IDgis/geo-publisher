package nl.idgis.publisher.database;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.StreamingQuery;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Stop;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class StreamingAutoCommit extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef target;
	
	private final StreamingQuery query;
	
	public StreamingAutoCommit(StreamingQuery query, ActorRef target) {
		this.query = query;
		this.target = target;
	}
	
	public static Props props(StreamingQuery query, ActorRef target) {
		return Props.create(StreamingAutoCommit.class, query, target);
	}
	
	private Procedure<Object> waitingForAck() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("transaction completed");
				
				getContext().stop(getSelf());
			}
			
		};
	}
	
	public Procedure<Object> waitingForStreamEnd(final ActorRef transaction) {
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
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof TransactionCreated) {
			log.debug("transaction created");
			
			ActorRef transaction = ((TransactionCreated) msg).getActor();
			transaction.tell(query, getSelf());
			
			getContext().become(waitingForStreamEnd(transaction));
		} else {
			unhandled(msg);
		}
	}
	
}
