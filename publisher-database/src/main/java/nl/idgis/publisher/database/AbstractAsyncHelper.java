package nl.idgis.publisher.database;

import com.mysema.query.sql.RelationalPath;

import scala.concurrent.ExecutionContext;

import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

public abstract class AbstractAsyncHelper implements AsyncHelper {

	protected final LoggingAdapter log;
	
	protected final ActorRef actor;
	
	protected final Timeout timeout;
	
	protected final ExecutionContext executionContext;
	
	AbstractAsyncHelper(ActorRef action, Timeout timeout, ExecutionContext executionContext, LoggingAdapter log) {
		this.actor = action;
		this.timeout = timeout;
		this.executionContext = executionContext;
		this.log = log;
	}
	
	@Override
	public final AsyncSQLQuery query() {
		return new AsyncSQLQuery(actor, timeout, executionContext);
	}

	@Override
	public final AsyncSQLInsertClause insert(RelationalPath<?> entity) {
		return new AsyncSQLInsertClause(actor, timeout, executionContext, entity);
	}
	
	@Override
	public final AsyncSQLUpdateClause update(RelationalPath<?> entity) {
		return new AsyncSQLUpdateClause(actor, timeout, executionContext, entity);
	}
}
