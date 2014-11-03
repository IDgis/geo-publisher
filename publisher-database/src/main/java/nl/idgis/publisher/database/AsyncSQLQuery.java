package nl.idgis.publisher.database;

import java.util.Iterator;

import nl.idgis.publisher.database.messages.PerformQuery;
import nl.idgis.publisher.utils.TypedList;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.dispatch.Mapper;
import akka.pattern.Patterns;
import akka.util.Timeout;

import com.mysema.query.DefaultQueryMetadata;
import com.mysema.query.NonUniqueResultException;
import com.mysema.query.QueryMetadata;
import com.mysema.query.Tuple;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Expression;
import com.mysema.query.types.template.NumberTemplate;

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
	public Future<TypedList<Tuple>> list(Expression<?>... args) {
		queryMixin.addProjection(args);
		
		return Patterns.ask(database, new PerformQuery(getMetadata()), timeout)
			.map(new Mapper<Object, TypedList<Tuple>>() {
				
				@Override
				@SuppressWarnings("unchecked")
				public TypedList<Tuple> apply(Object parameter) {
					return (TypedList<Tuple>)parameter;
				}
				
			}, executionContext);
	}

	@Override
	public <RT> Future<TypedList<RT>> list(Expression<RT> projection) {
		queryMixin.addProjection(projection);
		
		return Patterns.ask(database, new PerformQuery(getMetadata()), timeout)
			.map(new Mapper<Object, TypedList<RT>>() {
				
				@Override
				@SuppressWarnings("unchecked")
				public TypedList<RT> apply(Object parameter) {
					return (TypedList<RT>)parameter;
				}
				
			}, executionContext);
	}

	@Override
	public Future<TypedList<Tuple>> list(Object... args) {
		return list(asExpressions(args));
	}

	private Expression<?>[] asExpressions(Object... args) {
		Expression<?>[] exprArgs = new Expression<?>[args.length];
		
		int i = 0;
		for(Object arg : args) {
			if(arg instanceof Expression) {
				exprArgs[i++] = (Expression<?>)arg;
			} else {
				exprArgs[i++] = Expressions.constant(arg);
			}
		}
		return exprArgs;
	}
	
	private <T> Future<T> singleResult(Future<TypedList<T>> listResult) {
		return listResult.map(new Mapper<TypedList<T>, T>() {
			
			@Override
			public T checkedApply(TypedList<T> list) {
				Iterator<T> itr = list.iterator();
				
				if(itr.hasNext()) {
					return null;
				} else {
					T retval = itr.next();
					if(itr.hasNext()) {
						throw new NonUniqueResultException();
					} else {
						return retval;
					}
				}
			}
		}, executionContext);
	}

	@Override
	public Future<Tuple> singleResult(Expression<?>... args) {
		return singleResult(list(args));
	}

	@Override
	public <RT> Future<RT> singleResult(Expression<RT> projection) {
		return singleResult(list(projection));
	}

	@Override
	public Future<Tuple> singleResult(Object... args) {
		return singleResult(asExpressions(args));
	}

	@Override
	public Future<Boolean> exists() {
		return limit(1).singleResult(NumberTemplate.ONE).map(new Mapper<Integer, Boolean>() {
			
			@Override
			public Boolean apply(Integer i) {
				return i != null;
			}
			
		}, executionContext);
	}

	@Override
	public Future<Boolean> notExists() { 
		return exists().map(new Mapper<Boolean, Boolean>() {
			
			@Override
			public Boolean apply(Boolean b) {
				return !b;
			}
			
		}, executionContext);
	}
}
