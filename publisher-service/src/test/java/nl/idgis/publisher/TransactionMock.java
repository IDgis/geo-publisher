package nl.idgis.publisher;

import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Rollback;

import nl.idgis.publisher.protocol.messages.Ack;

public class TransactionMock extends UntypedActor {

	public static Props props() {
		return Props.create(TransactionMock.class);
	}
	
	protected void query(Object msg) {				
		unhandled(msg);		
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Commit || msg instanceof Rollback) {
			getSender().tell(new Ack(), getSelf());
			getContext().stop(getSelf());
		} else {
			query(msg);
		}
	}
}
