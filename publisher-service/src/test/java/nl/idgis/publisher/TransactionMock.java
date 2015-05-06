package nl.idgis.publisher;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.Rollback;

import nl.idgis.publisher.protocol.messages.Ack;

public class TransactionMock extends UntypedActor {
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public static Props props() {
		return Props.create(TransactionMock.class);
	}
	
	protected void handleQuery(Query query) throws Exception {				
		unhandled(query);		
	}

	@Override
	public final void onReceive(Object msg) throws Exception {
		if(msg instanceof Commit || msg instanceof Rollback) {
			getSender().tell(new Ack(), getSelf());
			getContext().stop(getSelf());
		} else if(msg instanceof Query) {
			handleQuery((Query)msg);
		} else {
			unhandled(msg);
		}
	}
}
