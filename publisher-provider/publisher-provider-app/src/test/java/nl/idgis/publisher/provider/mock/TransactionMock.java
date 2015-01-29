package nl.idgis.publisher.provider.mock;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
import nl.idgis.publisher.provider.database.messages.FetchTable;
import nl.idgis.publisher.provider.database.messages.PerformCount;
import nl.idgis.publisher.provider.mock.messages.PutTable;
import nl.idgis.publisher.recorder.messages.RecordedMessage;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class TransactionMock extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);	
	
	private final ActorRef recorder;
	
	public TransactionMock(ActorRef recorder) {
		this.recorder = recorder;
	}
	
	public static Props props(ActorRef recorder) {
		return Props.create(TransactionMock.class, recorder);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		recorder.tell(new RecordedMessage(getSelf(), getSender(), msg), getSelf());
		
		if(msg instanceof DescribeTable) {
			log.debug("describe table");
			
			getContext().parent().forward(msg, getContext());
		} else if(msg instanceof PerformCount) {
			log.debug("perform count");
			
			getContext().parent().forward(msg, getContext());
		} else if(msg instanceof PutTable) {
			log.debug("put table");
			
			getContext().parent().forward(msg, getContext());
		} else if(msg instanceof FetchTable) {
			log.debug("fetch table");
			
			getContext().parent().forward(msg, getContext());
		} else if(msg instanceof Rollback || msg instanceof Commit) {
			getSender().tell(new Ack(), getSelf());
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
	
}