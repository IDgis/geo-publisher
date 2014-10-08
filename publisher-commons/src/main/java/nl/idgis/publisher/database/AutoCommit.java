package nl.idgis.publisher.database;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.TransactionCreated;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class AutoCommit extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef target;
	private final Query query;
	
	public AutoCommit(Query query, ActorRef target) {
		this.query = query;
		this.target = target;
	}
	
	public static Props props(Query query, ActorRef target) {
		return Props.create(AutoCommit.class, query, target);
	}
	
	private Procedure<Object> waitingForAck(final Object answer) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("transaction completed");
				
				target.tell(answer, getContext().parent());
				
				getContext().stop(getSelf());
			}
			
		};
	}
	
	private Procedure<Object> waitingForAnswer(final ActorRef transaction) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				log.debug("query executed");
				
				transaction.tell(new Commit(), getSelf());
				
				getContext().become(waitingForAck(msg));
			}
			
		};
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof TransactionCreated) {
			log.debug("transaction created");
			
			ActorRef transaction = ((TransactionCreated) msg).getActor();
			transaction.tell(query, getSelf());
			
			getContext().become(waitingForAnswer(transaction));
		} else {
			unhandled(msg);
		}
	}
	
}
