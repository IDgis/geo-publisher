package nl.idgis.publisher.database;

import com.mysema.query.sql.RelationalPath;

import scala.concurrent.ExecutionContext;

import akka.actor.ActorRef;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

import nl.idgis.publisher.utils.FutureUtils;

public abstract class AbstractAsyncHelper implements AsyncHelper {

	protected final LoggingAdapter log;
	
	protected final ActorRef actor;
	
	protected final FutureUtils f;
	
	AbstractAsyncHelper(ActorRef action, FutureUtils f, LoggingAdapter log) {
		this.actor = action;
		this.f = f;		
		this.log = log;
	}
	
	@Override
	public final AsyncSQLQuery query() {
		return new AsyncSQLQuery(actor, f);
	}

	@Override
	public final AsyncSQLInsertClause insert(RelationalPath<?> entity) {
		return new AsyncSQLInsertClause(actor, f, entity);
	}
	
	@Override
	public final AsyncSQLUpdateClause update(RelationalPath<?> entity) {
		return new AsyncSQLUpdateClause(actor, f, entity);
	}
}
