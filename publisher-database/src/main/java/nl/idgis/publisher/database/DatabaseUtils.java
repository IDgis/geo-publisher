package nl.idgis.publisher.database;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import scala.Function1;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;

public class DatabaseUtils {

	public static <T, U> List<T> consumeList(ListIterator<Tuple> listIterator, U id, Expression<U> idPath, Function1<Tuple, T> mapper) {
		List<T> retval = new ArrayList<>();
		
		for(; listIterator.hasNext();) {
			Tuple tc = listIterator.next();
			
			U listId = tc.get(idPath);				
			if(!listId.equals(id)) {
				listIterator.previous();
				break;
			}
			
			retval.add(mapper.apply(tc));
		}
		
		return retval;
	}
}
