package nl.idgis.publisher.database;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.FutureUtils.Collector1;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.util.Timeout;

public class TransactionHandler {
	
	private final ActorRef transaction;
	
	private final Timeout timeout;
	
	private final ExecutionContext executionContext;

	TransactionHandler(ActorRef transaction, Timeout timeout, ExecutionContext executionContext) {
		this.transaction = transaction;
		this.timeout = timeout;
		this.executionContext = executionContext;
	}
	
	public AsyncSQLQuery query() {
		return new AsyncSQLQuery(transaction, timeout, executionContext);
	}
	
	public <T> Collector1<T> collect(Future<T> future) {
		return new FutureUtils(executionContext).collect(future);
	}
}
