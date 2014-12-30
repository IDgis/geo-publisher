package nl.idgis.publisher.database;

import nl.idgis.publisher.utils.SmartFuture;

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
	SmartFuture<Long> execute();
}
