package nl.idgis.publisher.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.types.Null;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Constant;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;
import com.mysema.query.types.SubQueryExpression;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.messages.PerformInsert;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class AsyncSQLInsertClause extends AbstractAsyncSQLClause<AsyncSQLInsertClause> implements AsyncInsertClause<AsyncSQLInsertClause> {
	
	private final RelationalPath<?> entity;	
	
	@Nullable
    private SubQueryExpression<?> subQuery;
	
	private final List<Path<?>> columns = new ArrayList<Path<?>>();

    private final List<Expression<?>> values = new ArrayList<Expression<?>>();
	
	public AsyncSQLInsertClause(ActorRef database, FutureUtils f, RelationalPath<?> entity) {
		super(database, f);
		
		this.entity = entity;
	}

	@Override
	public CompletableFuture<Long> execute() {
		Path<?>[] columnsArray = columns.toArray(new Path<?>[columns.size()]);
		Expression<?>[] valuesArray = values.toArray(new Expression<?>[values.size()]);
		
		return f.ask(database, new PerformInsert(entity, subQuery, columnsArray, valuesArray)).thenApply(TO_LONG);
	}
	
	@SuppressWarnings("unchecked")
	public <T> CompletableFuture<TypedList<T>> executeWithKeys(Path<T> path) {
		Path<?>[] columnsArray = columns.toArray(new Path<?>[columns.size()]);
		Expression<?>[] valuesArray = values.toArray(new Expression<?>[values.size()]);
		
		return f.ask(database, new PerformInsert(entity, subQuery, columnsArray, valuesArray, path)).thenApply(msg -> (TypedList<T>)msg);
	}
	
	public <T> CompletableFuture<Optional<T>> executeWithKey(Path<T> path) {
		return 
			executeWithKeys(path).thenApply(typedList -> {
				Iterator<T> itr = typedList.list().iterator();
				
				if(!itr.hasNext()) {
					return Optional.empty();
				}
				
				T t = itr.next();
				if(itr.hasNext()) {
					throw new IllegalStateException("insert query unexpectedly returned multiple keys");
				}
				
				return Optional.of(t);
			});
	}

	@Override
	public <T> AsyncSQLInsertClause set(Path<T> path, T value) {
		columns.add(path);
        if (value instanceof Expression<?>) {
            values.add((Expression<?>) value);
        } else if (value != null) {
            values.add(constExpression(value));
        } else {
            values.add(Null.CONSTANT);
        }
        return this;
	}

	@Override
	public <T> AsyncSQLInsertClause set(Path<T> path, Expression<? extends T> expression) {
		columns.add(path);
        values.add(expression);
        return this;
	}

	@Override
	public <T> AsyncSQLInsertClause setNull(Path<T> path) {
		columns.add(path);
        values.add(Null.CONSTANT);
        return this;
	}

	@Override
	public boolean isEmpty() {
		return values.isEmpty();
	}

	@Override
	public AsyncSQLInsertClause columns(Path<?>... columns) {
		this.columns.addAll(Arrays.asList(columns));
        return this;
	}

	@Override
	public AsyncSQLInsertClause select(SubQueryExpression<?> sq) {
		for(Expression<?> value : sq.getMetadata().getProjection()) {
			if(value instanceof Constant) {
				Object constant = ((Constant<?>)value).getConstant();
				
				if(!(constant instanceof Serializable)) {
					throw new IllegalArgumentException("query contains a non serializable constant: " + constant);
				}
			}
		}
		
		subQuery = sq;        
        return this;
	}

	@Override
	public AsyncSQLInsertClause values(Object... v) {
		for (Object value : v) {
            if (value instanceof Expression<?>) {
                values.add((Expression<?>) value);
            } else if (value != null) {
                values.add(constExpression(value));
            } else {
                values.add(Null.CONSTANT);
            }
        }
        return this;
	}
	
	private <T> Expression<T> constExpression(T value) {
		if(value instanceof Serializable) {
			return Expressions.constant(value);
		} else {
			throw new IllegalArgumentException("value is not serializable");
		}
	}

}
