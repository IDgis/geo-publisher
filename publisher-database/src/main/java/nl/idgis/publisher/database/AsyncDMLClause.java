package nl.idgis.publisher.database;

import java.util.concurrent.CompletableFuture;

/**
 * Parent interface for asynchronous DML clauses
 * 
 * @param <C> concrete subtype
 */
public interface AsyncDMLClause<C extends AsyncDMLClause<C>> {

	/**
     * Execute the clause and return the amount of affected rows
     *
     * @return
     */
	CompletableFuture<Long> execute();
}
