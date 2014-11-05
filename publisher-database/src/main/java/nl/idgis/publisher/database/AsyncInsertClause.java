package nl.idgis.publisher.database;

import com.mysema.query.types.Path;
import com.mysema.query.types.SubQueryExpression;

/**
 * InsertClause defines a generic interface for Insert clauses
 *
 * @param <C> concrete subtype
 */
public interface AsyncInsertClause<C extends AsyncInsertClause<C>> extends AsyncStoreClause<C> {

    /**
     * Define the columns to be populated
     *
     * @param columns
     * @return
     */
    C columns(Path<?>... columns);

    /**
     * Define the populate via subquery
     *
     * @param subQuery
     * @return
     */
    C select(SubQueryExpression<?> subQuery);

    /**
     * Define the value bindings
     *
     * @param v
     * @return
     */
    C values(Object... v);
}
