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
	
	private Procedure<Object> waitingForAck(final Object answer) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("transaction completed");
				
				target.tell(answer, getSelf());
				
				getContext().stop(getSelf());
			}
			
		};
	}
	
	private Procedure<Object> waitingForEnd(final ActorRef transaction, final ActorRef cursor) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("query executing");
				
				if(msg instanceof End) {				
					handleEnd(transaction, msg);
				} else if(msg instanceof Item) {
					handleItem(msg);
				} else if(msg instanceof NextItem) {
					handleNextItem(cursor, msg);
				} else if(msg instanceof Stop) {
					handleStop(transaction, cursor, msg);
				} else {
					unhandled(msg);
				}
			}			
		};			
	}
	
	private Procedure<Object> waitingForFirstMessage(final ActorRef transaction) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof End) {				
					handleEnd(transaction, msg);
				} else if(msg instanceof Item) {
					handleItem(msg);
					
					getContext().become(waitingForEnd(transaction, getSender()));
				} else {
					unhandled(msg);
				}
			}			
			
		};
	}
	
	private void handleStop(ActorRef transaction, ActorRef cursor, Object msg) {
		log.debug("stop");
		
		cursor.tell(msg, getSelf());
		
		transaction.tell(new Rollback(), getSelf());

		getContext().become(waitingForAck(msg));
	}
	
	private void handleItem(Object msg) {
		log.debug("item");
		
		target.tell(msg, getSelf());
	}

	private void handleEnd(ActorRef transaction, Object msg) {
		log.debug("end");
		
		transaction.tell(new Commit(), getSelf());

		getContext().become(waitingForAck(msg));
	}
	
	private void handleNextItem(ActorRef cursor, Object msg) {
		log.debug("next item");
		
		cursor.tell(msg, getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof TransactionCreated) {
			log.debug("transaction created");
			
			ActorRef transaction = ((TransactionCreated) msg).getActor();
			transaction.tell(query, getSelf());
			
			getContext().become(waitingForFirstMessage(transaction));
		} else {
			unhandled(msg);
		}
	}
	
}
