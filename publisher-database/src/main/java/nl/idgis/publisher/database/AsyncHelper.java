package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.RelationalPath;

import akka.actor.ActorRef;

public interface AsyncHelper {

	AsyncSQLQuery query();
	
	AsyncSQLInsertClause insert(RelationalPath<?> entity);
	
	AsyncSQLUpdateClause update(RelationalPath<?> entity);

	AsyncSQLDeleteClause delete(RelationalPath<?> entity);
	
	AsyncTransactionRef getTransactionRef();
	
	CompletableFuture<Object> ask(Object message);

	void tell(Object message, ActorRef sender);
}
