package nl.idgis.publisher.database;

import javax.annotation.Nullable;

import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;


/**
 * Parent interface for {@link AsyncInsertClause} and {@link AsyncUpdateClause}
 * 
 *
 * @param <C> concrete subtype
 */
public interface AsyncStoreClause<C extends AsyncStoreClause<C>> extends AsyncDMLClause<C> {

    /**
     * Add a value binding
     *
     * @param <T>
     * @param path path to be updated
     * @param value value to set
     * @return
     */
    <T> C set(Path<T> path, @Nullable T value);
    
    /**
     * Add an expression binding
     * 
     * @param <T>
     * @param path
     * @param expression
     * @return
     */
    <T> C set(Path<T> path, Expression<? extends T> expression);
    
    /**
     * Bind the given path to null
     * 
     * @param path
     * @return
     */
    <T> C setNull(Path<T> path);
    
    /**
     * Returns true, if no bindings have been set, otherwise false.
     * 
     * @return
     */
    boolean isEmpty();

}
