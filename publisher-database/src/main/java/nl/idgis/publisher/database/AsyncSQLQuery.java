package nl.idgis.publisher.database;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.DefaultQueryMetadata;
import com.mysema.query.NonUniqueResultException;
import com.mysema.query.QueryMetadata;
import com.mysema.query.Tuple;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Expression;
import com.mysema.query.types.template.NumberTemplate;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.messages.PerformQuery;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class AsyncSQLQuery extends AbstractAsyncSQLQuery<AsyncSQLQuery> implements Async {
		
	private final ActorRef database;
	
	private final FutureUtils f; 
	
	public AsyncSQLQuery(ActorRef database, FutureUtils f) {
		this(database, f, new DefaultQueryMetadata().noValidate());
	}
	
	private AsyncSQLQuery(ActorRef database, FutureUtils f, QueryMetadata metadata) {
		super(metadata);		
		
		this.database = database;
		this.f = f;
	}

	@Override
	public AsyncSQLQuery clone() {
		return new AsyncSQLQuery(database, f, getMetadata().clone());
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public CompletableFuture<TypedList<Tuple>> list(Expression<?>... args) {
		queryMixin.addProjection(args);
		
		return f.ask(database, new PerformQuery(getMetadata())).thenApply(msg -> {
			if(msg instanceof Failure) {
				throw new RuntimeException(((Failure) msg).getCause());
			}
			
			return (TypedList<Tuple>)msg;
		});
	}
	
	public CompletableFuture<Long> count() {
		return f.ask(database, new PerformQuery(getMetadata())).thenApply(msg -> {
			if(msg instanceof Failure) {
				throw new RuntimeException(((Failure) msg).getCause());
			}
			
			return (Long)msg;
		});
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <RT> CompletableFuture<TypedList<RT>> list(Expression<RT> projection) {
		queryMixin.addProjection(projection);
		
		return f.ask(database, new PerformQuery(getMetadata())).thenApply(msg -> {
			if(msg instanceof Failure) {
				throw new RuntimeException(((Failure) msg).getCause());
			}
			
			return (TypedList<RT>)msg;
		});
	}

	@Override
	public CompletableFuture<TypedList<Tuple>> list(Object... args) {
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
	
	private <T> CompletableFuture<Optional<T>> singleResult(CompletableFuture<TypedList<T>> listResult) {
		return listResult.thenApply(list -> {
			Iterator<T> itr = list.iterator();
			
			if(itr.hasNext()) {					
				T retval = itr.next();
				if(itr.hasNext()) {
					throw new NonUniqueResultException();
				} else {
					return Optional.of(retval);
				}
			} else {
				return Optional.empty();
			}
		});
	}

	@Override
	public CompletableFuture<Optional<Tuple>> singleResult(Expression<?>... args) {
		return singleResult(list(args));
	}

	@Override
	public <RT> CompletableFuture<Optional<RT>> singleResult(Expression<RT> projection) {
		return singleResult(list(projection));
	}

	@Override
	public CompletableFuture<Optional<Tuple>> singleResult(Object... args) {
		return singleResult(asExpressions(args));
	}

	@Override
	public CompletableFuture<Boolean> exists() {
		return limit(1).singleResult(NumberTemplate.ONE).thenApply(i -> i.isPresent());
	}

	@Override
	public CompletableFuture<Boolean> notExists() { 
		return exists().thenApply(b -> !b);
	}
}
