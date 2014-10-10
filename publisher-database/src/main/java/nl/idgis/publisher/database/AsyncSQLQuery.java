package nl.idgis.publisher.database;

import nl.idgis.publisher.database.messages.PerformQuery;
import nl.idgis.publisher.utils.TypedIterable;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.mysema.query.DefaultQueryMetadata;
import com.mysema.query.QueryMetadata;
import com.mysema.query.Tuple;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Expression;

public class AsyncSQLQuery extends AbstractAsyncSQLQuery<AsyncSQLQuery> implements Async {
	
	private final ActorRef database;	
	private final Timeout timeout;
	private final ExecutionContext executionContext; 
	
	public AsyncSQLQuery(ActorRef database, Timeout timeout, ExecutionContext executionContext) {
		this(database, timeout, executionContext, new DefaultQueryMetadata().noValidate());
	}
	
	private AsyncSQLQuery(ActorRef database, Timeout timeout, ExecutionContext executionContext, QueryMetadata metadata) {
		super(metadata);		
		
		this.database = database;
		this.timeout = timeout;
		this.executionContext = executionContext;
	}

	@Override
	public AsyncSQLQuery clone() {
		return new AsyncSQLQuery(database, timeout, executionContext, getMetadata().clone());
	}

	@Override
	public Future<TypedIterable<Tuple>> list(Expression<?>... args) {
		queryMixin.addProjection(args);
		
		return Patterns.ask(database, new PerformQuery(getMetadata()), timeout)
			.map(new Mapper<Object, TypedIterable<Tuple>>() {
				
				@Override
				@SuppressWarnings("unchecked")
				public TypedIterable<Tuple> apply(Object parameter) {
					return (TypedIterable<Tuple>)parameter;
				}
				
			}, executionContext);
	}

	@Override
	public <RT> Future<TypedIterable<RT>> list(Expression<RT> projection) {
		queryMixin.addProjection(projection);
		
		return Patterns.ask(database, new PerformQuery(getMetadata()), timeout)
			.map(new Mapper<Object, TypedIterable<RT>>() {
				
				@Override
				@SuppressWarnings("unchecked")
				public TypedIterable<RT> apply(Object parameter) {
					return (TypedIterable<RT>)parameter;
				}
				
			}, executionContext);
	}

	@Override
	public Future<TypedIterable<Tuple>> list(Object... args) {
		Expression<?>[] exprArgs = new Expression<?>[args.length];
		
		int i = 0;
		for(Object arg : args) {
			if(arg instanceof Expression) {
				exprArgs[i++] = (Expression<?>)arg;
			} else {
				exprArgs[i++] = Expressions.constant(arg);
			}
		}
		
		return list(exprArgs);
	}

}
