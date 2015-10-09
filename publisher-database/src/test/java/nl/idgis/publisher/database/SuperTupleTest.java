package nl.idgis.publisher.database;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;

public class SuperTupleTest {
	
	JoinedTuple superTuple;
	
	Expression<Integer> id;
	
	Expression<String> stringValue;
	
	Expression<Double> doubleValue;
	
	@Before
	@SuppressWarnings("unchecked")
	public void createSuperTuple() {
		id = (Expression<Integer>)mock(Expression.class);
		stringValue = (Expression<String>)mock(Expression.class);
		doubleValue = (Expression<Double>)mock(Expression.class);
				
		Tuple parentTuple = mock(Tuple.class);
		when(parentTuple.size()).thenReturn(2);
		when(parentTuple.toArray()).thenReturn(new Object[]{0, "Hello, world!"});
		when(parentTuple.get(id)).thenReturn(0);
		when(parentTuple.get(stringValue)).thenReturn("Hello, world!");		
		when(parentTuple.get(0, Integer.class)).thenReturn(0);
		when(parentTuple.get(1, String.class)).thenReturn("Hello, world!");
		
		Tuple tuple = mock(Tuple.class);
		when(tuple.size()).thenReturn(2);
		when(tuple.toArray()).thenReturn(new Object[]{0, 42.0});
		when(tuple.get(id)).thenReturn(0);
		when(tuple.get(doubleValue)).thenReturn(42.0);
		when(tuple.get(0, Integer.class)).thenReturn(0);
		when(tuple.get(1, Double.class)).thenReturn(42.0);
		
		superTuple = new JoinedTuple(parentTuple, tuple);
	}

	@Test
	public void testSize() {
		assertEquals(4, superTuple.size());
	}
	
	@Test
	public void testToArray() {
		Object[] array = superTuple.toArray();
		assertEquals(4, array.length);
		assertEquals(Integer.valueOf(0), array[0]);
		assertEquals("Hello, world!", array[1]);
		assertEquals(Integer.valueOf(0), array[2]);
		assertEquals(Double.valueOf(42.0), array[3]);
	}
	
	@Test
	public void testGet() {
		assertEquals(Integer.valueOf(0), superTuple.get(id));
		assertEquals("Hello, world!", superTuple.get(stringValue));		
		assertEquals(Double.valueOf(42.0), superTuple.get(doubleValue));
	}
	
	@Test
	public void testGetIndex() {
		assertEquals(Integer.valueOf(0), superTuple.get(0, Integer.class));
		assertEquals("Hello, world!", superTuple.get(1, String.class));
		assertEquals(Integer.valueOf(0), superTuple.get(2, Integer.class));
		assertEquals(Double.valueOf(42.0), superTuple.get(3, Double.class));
	}
}
