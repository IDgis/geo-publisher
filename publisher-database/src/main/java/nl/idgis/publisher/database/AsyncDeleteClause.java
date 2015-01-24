package nl.idgis.publisher.database;

import com.mysema.query.FilteredClause;

/**
 * AsyncDeleteClause defines a generic interface for Delete clauses
 *
 * @param <C> concrete subtype
 */
public interface AsyncDeleteClause<C extends AsyncDeleteClause<C>> extends AsyncDMLClause<C>, FilteredClause<C> {

    
}
