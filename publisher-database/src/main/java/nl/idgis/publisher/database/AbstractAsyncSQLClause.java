package nl.idgis.publisher.database;

import scala.concurrent.ExecutionContext;
import akka.actor.ActorRef;
import akka.util.Timeout;

public abstract class AbstractAsyncSQLClause<C extends AbstractAsyncSQLClause<C>> implements AsyncDMLClause<C> {

	protected final ActorRef database;
    
	protected final Timeout timeout;
    
	protected final ExecutionContext executionContext;
    
    protected AbstractAsyncSQLClause(ActorRef database, Timeout timeout, ExecutionContext executionContext) {
		this.database = database;
		this.timeout = timeout;
		this.executionContext = executionContext;
	}
}
