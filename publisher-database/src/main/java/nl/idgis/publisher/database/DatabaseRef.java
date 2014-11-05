package nl.idgis.publisher.database;

import com.mysema.query.sql.RelationalPath;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Ack;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.UntypedActorContext;
import akka.dispatch.Mapper;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class DatabaseRef {
	
	private final LoggingAdapter log;

	private final ActorRef actor;
	
	private final Timeout timeout;
	
	private final ExecutionContext executionContext;
	
	public DatabaseRef(ActorRef actor, Timeout timeout, ExecutionContext executionContext, LoggingAdapter log) {
		this.actor = actor;
		this.timeout = timeout;
		this.executionContext = executionContext;
		this.log = log;
	}
	
	public Future<Object> transactional(final Function<TransactionHandler, Future<Object>> handler) {
		return Patterns.ask(actor, new StartTransaction(), timeout)
			.flatMap(new Mapper<Object, Future<Object>>() {

				@Override
				public Future<Object> checkedApply(Object msg) throws Exception {
					if(msg instanceof TransactionCreated) {
						log.debug("transaction created");
						
						final ActorRef transaction = ((TransactionCreated)msg).getActor();
						return handler.apply(new TransactionHandler(transaction, timeout, executionContext)).flatMap(new Mapper<Object, Future<Object>>() {
							
							public Future<Object> checkedApply(final Object result) {
								log.debug("query result obtained");
								
								return Patterns.ask(transaction, new Commit(), timeout)
									.map(new Mapper<Object, Object>() {
										
										public Object apply(Object msg) {
											if(msg instanceof Ack) {
												log.debug("committed");
												
												if(result != null) {
													return result;
												}
												
												return new Ack();
											} else {
												log.debug("commit failed");
												
												return msg;
											}
										}
										
									}, executionContext);
							}
						}, executionContext);
					} else {
						throw new IllegalArgumentException("TransactionCreated expected");
					}
				}				
			}, executionContext);			
	}
	
	public AsyncSQLQuery query() {
		return new AsyncSQLQuery(actor, timeout, executionContext);
	}
	
	public AsyncSQLInsertClause insert(RelationalPath<?> entity) {
		return new AsyncSQLInsertClause(actor, timeout, executionContext, entity);
	}

	// TODO: remove method
	public void forward(Object msg, UntypedActorContext context) {
		actor.forward(msg, context);
	}
}
