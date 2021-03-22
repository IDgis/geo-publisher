package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.RelationalPath;

import akka.actor.ActorRef;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.utils.FutureUtils;

public abstract class AbstractAsyncHelper implements AsyncHelper {

	protected final LoggingAdapter log;
	
	protected final ActorRef actorRef;
	
	protected final String origin;
	
	protected final FutureUtils f;
	
	AbstractAsyncHelper(ActorRef actorRef, String origin, FutureUtils f, LoggingAdapter log) {
		this.actorRef = actorRef;
		this.origin = origin;
		this.f = f;		
		this.log = log;
	}
	
	@Override
	public final AsyncSQLQuery query() {
		return new AsyncSQLQuery(actorRef, f);
	}

	@Override
	public final AsyncSQLInsertClause insert(RelationalPath<?> entity) {
		return new AsyncSQLInsertClause(actorRef, f, entity);
	}
	
	@Override
	public final AsyncSQLUpdateClause update(RelationalPath<?> entity) {
		return new AsyncSQLUpdateClause(actorRef, f, entity);
	}
	
	@Override
	public final AsyncSQLDeleteClause delete(RelationalPath<?> entity) {
		return new AsyncSQLDeleteClause(actorRef, f, entity);
	}
	
	@Override
	public final CompletableFuture<Object> ask(Object message) {
		return f.ask(actorRef, message);
	}
	
	@Override
	public final CompletableFuture<Object> ask(Object message, long timeout) {
		return f.ask(actorRef, message, timeout);
	}
	
	@Override
	public final void tell(Object message, ActorRef sender) {
		actorRef.tell(message, sender);
	}
}
