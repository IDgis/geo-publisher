package nl.idgis.publisher.database;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.protocol.messages.Failure;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Terminated;
import akka.japi.Procedure;

public class AutoCommit extends AbstractAutoCommit<Query> {
	
	private Object answer = null;
	
	public AutoCommit(Query query, ActorRef target) {
		super(query, target);
	}
	
	public static Props props(Query query, ActorRef target) {
		return Props.create(AutoCommit.class, query, target);
	}
	
	private Procedure<Object> waitingForAnswer() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Failure) {
					handleFailure((Failure)msg);
				} else if(msg instanceof ReceiveTimeout) {
					handleTimeout();
				} else if(msg instanceof Terminated) {
					handleTerminated((Terminated)msg);
				} else {
					log.debug("query executed");
					transaction.tell(new Commit(), getSelf());
					
					answer = msg;
				
					getContext().become(waitingForAck());
				}
			}
			
		};
	}
	
	@Override
	protected void onCompleted() {
		if(answer != null) {
			target.tell(answer, getContext().parent());
		}
	};
	
	@Override
	protected void onStarted() {
		getContext().become(waitingForAnswer());
	}
}
