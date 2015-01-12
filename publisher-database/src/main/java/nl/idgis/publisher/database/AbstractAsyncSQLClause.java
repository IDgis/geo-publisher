package nl.idgis.publisher.database;

import java.util.function.Function;

import akka.actor.ActorRef;

import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

public abstract class AbstractAsyncSQLClause<C extends AbstractAsyncSQLClause<C>> implements AsyncDMLClause<C> {
	
	protected final static Function<Object, Long> TO_LONG = msg -> {
		if(msg instanceof Failure) {
			throw new RuntimeException(((Failure)msg).getCause());
		}
		
		return (Long)msg;
	};

	protected final ActorRef database;
    
	protected final FutureUtils f;
    
    protected AbstractAsyncSQLClause(ActorRef database, FutureUtils f) {
		this.database = database;
		this.f = f;		
	}
}
