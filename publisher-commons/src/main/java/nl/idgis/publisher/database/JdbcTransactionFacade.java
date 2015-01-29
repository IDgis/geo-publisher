package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.utils.FutureUtils;

public class JdbcTransactionFacade implements Transaction<ActorRef> {

	private final ActorRef actorRef;
	
	private final FutureUtils f;
	
	public JdbcTransactionFacade(ActorRef actorRef, FutureUtils f) {
		this.actorRef = actorRef;
		this.f = f;
	}
	
	@Override
	public <U> CompletableFuture<U> apply(Function<ActorRef, CompletableFuture<U>> handler) {
		return handler.apply(actorRef);
	}

	@Override
	public CompletableFuture<Ack> commit() {		
		return f.ask(actorRef, new Commit(), Ack.class);
	}

	@Override
	public CompletableFuture<Ack> rollback() {
		return f.ask(actorRef, new Rollback(), Ack.class);
	}

}
