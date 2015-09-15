package nl.idgis.publisher.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;
import com.mysema.query.types.QTuple;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
	
	@Test
	@SuppressWarnings("unchecked")
	public void testJoin() {
		Expression<Integer> idExpr = mock(Expression.class);
		Expression<String> firstExpr = mock(Expression.class);
		Expression<String> secondExpr = mock(Expression.class);
		Expression<String> thirdExpr = mock(Expression.class);
		
		QTuple firstTuple = new QTuple(idExpr, firstExpr);
		QTuple secondTuple = new QTuple(idExpr, secondExpr);
		QTuple thirdTuple = new QTuple(idExpr, thirdExpr);
		
		Iterator<Tuple> actual = DatabaseUtils		
			.join(
				DatabaseUtils.join(
					Stream.of(
						firstTuple.newInstance(0, "zero"),
						firstTuple.newInstance(1, "one"),
						firstTuple.newInstance(2, "two")),
					Stream.of(
						secondTuple.newInstance(1, "a"),
						secondTuple.newInstance(1, "b"),
						secondTuple.newInstance(2, "c"),
						secondTuple.newInstance(2, "d"),
						secondTuple.newInstance(2, "e"),
						secondTuple.newInstance(3, "e")),
					idExpr),
					Stream.of(
						thirdTuple.newInstance(1, "Z"),
						thirdTuple.newInstance(2, "Y")),
					idExpr)
			.iterator();
		
		QTuple expectedTuple = new QTuple(idExpr, firstExpr, idExpr, secondExpr, idExpr, thirdExpr);
		
		Iterator<Tuple> expected = Stream
			.of(
				expectedTuple.newInstance(1, "one", 1, "a", 1, "Z"),
				expectedTuple.newInstance(1, "one", 1, "b", 1, "Z"),
				expectedTuple.newInstance(2, "two", 2, "c", 2, "Y"),
				expectedTuple.newInstance(2, "two", 2, "d", 2, "Y"),
				expectedTuple.newInstance(2, "two", 2, "e", 2, "Y"))
			.iterator();
		
		while(expected.hasNext()) {
			assertTrue(actual.hasNext());
			assertEquals(expected.next(), actual.next());
		}
		
		assertFalse(actual.hasNext());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testLeftJoin() {
		Expression<Integer> idExpr = mock(Expression.class);
		Expression<String> firstExpr = mock(Expression.class);
		Expression<String> secondExpr = mock(Expression.class);
		
		QTuple firstTuple = new QTuple(idExpr, firstExpr);
		QTuple secondTuple = new QTuple(idExpr, secondExpr);
		
		Iterator<Tuple> actual = DatabaseUtils
			.leftJoin(
				Stream.of(
					firstTuple.newInstance(0, "zero"),
					firstTuple.newInstance(1, "one"),
					firstTuple.newInstance(2, "two")),
				Stream.of(
					secondTuple.newInstance(1, "a"),
					secondTuple.newInstance(1, "b"),
					secondTuple.newInstance(2, "c"),
					secondTuple.newInstance(2, "d"),
					secondTuple.newInstance(2, "e"),
					secondTuple.newInstance(3, "f")),
				idExpr)
			.iterator();
		
		QTuple expectedTuple = new QTuple(idExpr, firstExpr, idExpr, secondExpr);
		
		Iterator<Tuple> expected = Stream
			.of(
				expectedTuple.newInstance(0, "zero", null, null),
				expectedTuple.newInstance(1, "one", 1, "a"),
				expectedTuple.newInstance(1, "one", 1, "b"),
				expectedTuple.newInstance(2, "two", 2, "c"),
				expectedTuple.newInstance(2, "two", 2, "d"),
				expectedTuple.newInstance(2, "two", 2, "e"))
			.iterator();
		
		while(expected.hasNext()) {
			assertTrue(actual.hasNext());
			assertEquals(expected.next(), actual.next());
		}
		
		assertFalse(actual.hasNext());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testJoinCollection() {
		Expression<Integer> idExpr = mock(Expression.class);
		Expression<String> firstExpr = mock(Expression.class);
		Expression<String> secondExpr = mock(Expression.class);
		
		QTuple firstTuple = new QTuple(idExpr, firstExpr);
		QTuple secondTuple = new QTuple(idExpr, secondExpr);
		
		Iterator<Tuple> itr = DatabaseUtils
			.join(
				Collections.singletonList(firstTuple.newInstance(0, "a")), 
				Collections.singletonList(secondTuple.newInstance(0, "A")), 
				idExpr)
			.iterator();
		
		assertTrue(itr.hasNext());
		Tuple t = itr.next();
		assertArrayEquals(new Object[]{0, "a", 0, "A"}, t.toArray());
		assertFalse(itr.hasNext());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testJoinCollectionEmpty() {
		Collection<Tuple> left = mock(Collection.class);
		when(left.isEmpty()).thenReturn(true);
		
		Collection<Tuple> right = mock(Collection.class);
		when(right.isEmpty()).thenReturn(true);
		
		Iterator<Tuple> itr = DatabaseUtils
			.join(
				left, 
				right, 
				null)
			.iterator();
		
		assertFalse(itr.hasNext());
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testJoinCollectionLeftEmpty() {
		Collection<Tuple> left = mock(Collection.class);
		when(left.isEmpty()).thenReturn(true);
		
		Collection<Tuple> right = Collections.singleton(mock(Tuple.class));
		
		Collection<Tuple> result = DatabaseUtils
			.join(
				left, 
				right, 
				null)
			.collect(Collectors.toSet());
		
		assertEquals(right, result);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testJoinCollectionRightEmpty() {
		Collection<Tuple> left = Collections.singleton(mock(Tuple.class));
		
		Collection<Tuple> right = mock(Collection.class);
		when(right.isEmpty()).thenReturn(true);
		
		Collection<Tuple> result = DatabaseUtils
			.join(
				left, 
				right, 
				null)
			.collect(Collectors.toSet());
			
		assertEquals(left, result);
	}
}
