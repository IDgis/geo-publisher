package nl.idgis.publisher.database;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnegative;

import com.mysema.query.DefaultQueryMetadata;
import com.mysema.query.JoinType;
import com.mysema.query.QueryFlag;
import com.mysema.query.QueryFlag.Position;
import com.mysema.query.QueryMetadata;
import com.mysema.query.QueryModifiers;
import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.types.Null;
import com.mysema.query.types.ConstantImpl;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;

import scala.concurrent.ExecutionContext;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;

import nl.idgis.publisher.database.messages.PerformUpdate;
import nl.idgis.publisher.utils.SmartFuture;

public class AsyncSQLUpdateClause extends AbstractAsyncSQLClause<AsyncSQLUpdateClause> implements AsyncUpdateClause<AsyncSQLUpdateClause> {
	
	private final RelationalPath<?> entity;

	private final List<Path<?>> columns = new ArrayList<Path<?>>();

    private final List<Expression<?>> values = new ArrayList<Expression<?>>();

    private QueryMetadata metadata = new DefaultQueryMetadata();    

    public AsyncSQLUpdateClause(ActorRef database, Timeout timeout, ExecutionContext executionContext, RelationalPath<?> entity) {
		super(database, timeout, executionContext);
		
		this.entity = entity;
		metadata.addJoin(JoinType.DEFAULT, entity);
	}

    /**
     * Add the given String literal at the given position as a query flag
     *
     * @param position
     * @param flag
     * @return
     */
    public AsyncSQLUpdateClause addFlag(Position position, String flag) {
        metadata.addFlag(new QueryFlag(position, flag));
        return this;
    }

    /**
     * Add the given Expression at the given position as a query flag
     *
     * @param position
     * @param flag
     * @return
     */
    public AsyncSQLUpdateClause addFlag(Position position, Expression<?> flag) {
        metadata.addFlag(new QueryFlag(position, flag));
        return this;
    }
   
    @Override
    public <T> AsyncSQLUpdateClause set(Path<T> path, T value) {
        if (value instanceof Expression<?>) {
        	columns.add(path);
        	values.add((Expression<?>)value);
        } else if (value != null) {
        	columns.add(path);
        	values.add(ConstantImpl.create(value));
        } else {
            setNull(path);
        }
        return this;
    }

    @Override
    public <T> AsyncSQLUpdateClause set(Path<T> path, Expression<? extends T> expression) {
        if (expression != null) {
        	columns.add(path);
        	values.add(expression);
        } else {
            setNull(path);
        }
        return this;
    }

    @Override
    public <T> AsyncSQLUpdateClause setNull(Path<T> path) {
    	columns.add(path);
    	values.add(Null.CONSTANT);
    	
        return this;
    }

    @Override
    public AsyncSQLUpdateClause set(List<? extends Path<?>> paths, List<?> values) {
        for (int i = 0; i < paths.size(); i++) {
        	this.columns.add(paths.get(i));
        	
            if (values.get(i) instanceof Expression) {            	
            	this.values.add((Expression<?>)values.get(i));                
            } else if (values.get(i) != null) {            	
            	this.values.add(ConstantImpl.create(values.get(i)));                
            } else {
            	this.values.add(Null.CONSTANT);                
            }
        }
        return this;
    }

    public AsyncSQLUpdateClause where(Predicate p) {
        metadata.addWhere(p);
        return this;
    }

    @Override
    public AsyncSQLUpdateClause where(Predicate... o) {
        for (Predicate p : o) {
            metadata.addWhere(p);
        }
        return this;
    }

    public AsyncSQLUpdateClause limit(@Nonnegative long limit) {
        metadata.setModifiers(QueryModifiers.limit(limit));
        return this;
    }

    @Override
    public boolean isEmpty() {
        return columns.isEmpty();
    }

	@Override
	public SmartFuture<Long> execute() {
		return new SmartFuture<>(Patterns.ask(database, new PerformUpdate(entity, columns, values, metadata), timeout)
			.map(TO_LONG, executionContext), executionContext);
	}
}
