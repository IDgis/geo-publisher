package nl.idgis.publisher.database;

import java.util.List;

import com.mysema.query.FilteredClause;
import com.mysema.query.types.Path;

/**
 * UpdateClause defines a generic interface for Update clauses
 *
 * @param <C> concrete subtype
 */
public interface AsyncUpdateClause<C extends AsyncUpdateClause<C>> extends AsyncStoreClause<C>, FilteredClause<C> {

	C set(List<? extends Path<?>> paths, List<?> values);
}
