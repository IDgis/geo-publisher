package nl.idgis.publisher;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.JdbcTransactionSupplier;
import nl.idgis.publisher.database.TransactionHandler;
import nl.idgis.publisher.database.messages.Query;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

public class DatabaseMock extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Props transactionProps;
	
	private TransactionHandler<ActorRef> transactionHandler;
	
	private FutureUtils f;
	
	public DatabaseMock(Props transactionProps) {
		this.transactionProps = transactionProps;
	}
	
	public void preStart() {
		f = new FutureUtils(getContext());
		
		transactionHandler = new TransactionHandler<>(new JdbcTransactionSupplier(getSelf(), f), log);
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
			log.debug("starting transaction");
			
			getSender().tell(new TransactionCreated(
				getContext().actorOf(transactionProps)), getSelf());
		} else if(msg instanceof Query) {
			log.debug("executing query in auto-commit mode");
			
			ActorRef sender = getSender(), self = getSelf();
			transactionHandler.transactional(tx -> f.ask(tx, msg)).whenComplete((result, t) -> {
				if(t == null) {
					sender.tell(result, self);
				} else {
					sender.tell(new Failure(t), self);
				}
			});
		} else {
			unhandled(msg);
		}
	}
	
}

