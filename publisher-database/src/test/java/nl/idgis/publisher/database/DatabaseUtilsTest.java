package nl.idgis.publisher.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.function.Function;

import org.junit.Test;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DatabaseUtilsTest {
	
	@Test
	@SuppressWarnings("unchecked")
	public void testConsumeList() {
		Expression<Integer> idExpr = mock(Expression.class);
		Expression<String> valueExpr = mock(Expression.class);
		
		List<Tuple> tuples = new ArrayList<Tuple>();
		
		for(int i = 0; i < 10; i++) {
			Tuple a = mock(Tuple.class);
			when(a.get(idExpr)).thenReturn(i);
			when(a.get(valueExpr)).thenReturn("" + i + "a");
			
			Tuple b = mock(Tuple.class);
			when(b.get(idExpr)).thenReturn(i);
			when(b.get(valueExpr)).thenReturn("" + i + "b");
			
			tuples.add(a);
			tuples.add(b);
		}
		
		Function<Tuple, String> mapper = t -> t.get(valueExpr);
		
		ListIterator<Tuple> itr = tuples.listIterator();
		
		assertEquals(Arrays.asList("2a", "2b"), DatabaseUtils.consumeList(itr, 2, idExpr, mapper));
		assertEquals(Arrays.asList("5a", "5b"), DatabaseUtils.consumeList(itr, 5, idExpr, mapper));
		assertEquals(Collections.emptyList(), DatabaseUtils.consumeList(itr, 10, idExpr, mapper));
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected=IllegalArgumentException.class)
	public void testDetectedWrongOrder() {
		Expression<Integer> idExpr = mock(Expression.class);
		Expression<String> valueExpr = mock(Expression.class);
		
		List<Tuple> tuples = new ArrayList<Tuple>();
		
		Random r = new Random(0);
		for(int i = 0; i < 10; i++) {
			Tuple a = mock(Tuple.class);
			when(a.get(idExpr)).thenReturn(r.nextInt());
			when(a.get(valueExpr)).thenReturn("" + i + "a");
			
			Tuple b = mock(Tuple.class);
			when(b.get(idExpr)).thenReturn(i);
			when(b.get(valueExpr)).thenReturn("" + i + "b");
			
			tuples.add(a);
			tuples.add(b);
		}		
			
		ListIterator<Tuple> itr = tuples.listIterator();
		DatabaseUtils.consumeList(itr, 42, idExpr, t -> t.get(valueExpr));
	}
}
