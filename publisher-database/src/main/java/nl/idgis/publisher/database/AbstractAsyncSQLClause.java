package nl.idgis.publisher.database;

import scala.concurrent.ExecutionContext;

import akka.actor.ActorRef;
import akka.dispatch.Mapper;
import akka.util.Timeout;

import nl.idgis.publisher.protocol.messages.Failure;

public abstract class AbstractAsyncSQLClause<C extends AbstractAsyncSQLClause<C>> implements AsyncDMLClause<C> {
	
	protected final static Mapper<Object, Long> TO_LONG = new Mapper<Object, Long>() {
		
		@Override
		public Long checkedApply(Object o) throws Throwable {
			if(o instanceof Failure) {
				throw ((Failure) o).getCause();
			}
			
			return (Long)o;
		}		
	};

	protected final ActorRef database;
    
	protected final Timeout timeout;
    
	protected final ExecutionContext executionContext;
    
    protected AbstractAsyncSQLClause(ActorRef database, Timeout timeout, ExecutionContext executionContext) {
		this.database = database;
		this.timeout = timeout;
		this.executionContext = executionContext;
	}
}
