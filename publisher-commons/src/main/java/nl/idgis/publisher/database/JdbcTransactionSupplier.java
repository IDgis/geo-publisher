package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.utils.FutureUtils;

public class JdbcTransactionSupplier implements TransactionSupplier<ActorRef> {
	
	private final ActorRef actorRef;
	
	private final FutureUtils f;
	
	public JdbcTransactionSupplier(ActorRef actorRef, FutureUtils f) {
		this.actorRef = actorRef;
		this.f = f;
	}

	@Override
	public CompletableFuture<? extends Transaction<ActorRef>> transaction() {
		return f.ask(actorRef, new StartTransaction(), TransactionCreated.class).thenApply(transactionCreated ->
			new JdbcTransactionFacade(transactionCreated.getActor(), f));
	}

}
