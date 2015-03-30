package nl.idgis.publisher;

import akka.actor.Props;
import akka.actor.UntypedActor;

import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;

public class DatabaseMock extends UntypedActor {
	
	private final Props transactionProps;
	
	public DatabaseMock(Props transactionProps) {
		this.transactionProps = transactionProps;
	}
	
	public static Props props() {
		return props(TransactionMock.props());
	}
		
	public static Props props(Props transactionProps) {
		return Props.create(DatabaseMock.class, transactionProps);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof StartTransaction) {
			getSender().tell(new TransactionCreated(
				getContext().actorOf(transactionProps)), getSelf());
		} else {
			unhandled(msg);
		}
	}
	
}

