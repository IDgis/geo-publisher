package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;

import com.mysema.query.DefaultQueryMetadata;
import com.mysema.query.JoinType;
import com.mysema.query.sql.RelationalPath;
import com.mysema.query.types.Predicate;

import akka.actor.ActorRef;

import nl.idgis.publisher.database.messages.PerformDelete;
import nl.idgis.publisher.utils.FutureUtils;

public class AsyncSQLDeleteClause extends AbstractAsyncSQLClause<AsyncSQLDeleteClause> implements AsyncDeleteClause<AsyncSQLDeleteClause> {
	
	private final RelationalPath<?> entity;
	
	private final DefaultQueryMetadata metadata = new DefaultQueryMetadata();

	protected AsyncSQLDeleteClause(ActorRef database, FutureUtils f, RelationalPath<?> entity) {
		super(database, f);
		
		this.entity = entity;
		metadata.addJoin(JoinType.DEFAULT, entity);
	}	
	
	public AsyncSQLDeleteClause where(Predicate p) {
        metadata.addWhere(p);
        
        return this;
    }

    @Override
    public AsyncSQLDeleteClause where(Predicate... o) {
        for (Predicate p : o) {
            metadata.addWhere(p);
        }
        
        return this;
    }	
	
	@Override
	public CompletableFuture<Long> execute() {
		return f.ask(database, new PerformDelete(entity, metadata)).thenApply(TO_LONG);
	}

}
