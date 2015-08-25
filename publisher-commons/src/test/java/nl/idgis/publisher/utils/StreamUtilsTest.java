package nl.idgis.publisher.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;

import nl.idgis.publisher.utils.StreamUtils.IndexedEntry;
import nl.idgis.publisher.utils.StreamUtils.ZippedEntry;

import static nl.idgis.publisher.utils.StreamUtils.zip;
import static nl.idgis.publisher.utils.StreamUtils.index;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StreamUtilsTest {

	@Test
	public void testZip() {
		Iterator<ZippedEntry<Integer, String>> i =
			zip(
				Arrays.asList(0, 1, 2, 3).stream(), 
				Arrays.asList("Hello", "world").stream())
					.iterator();
		
		assertTrue(i.hasNext());
		
		ZippedEntry<Integer, String> first = i.next();
		assertNotNull(first);
		
		assertEquals(Integer.valueOf(0), first.getFirst());
		assertEquals("Hello", first.getSecond());
		
		assertTrue(i.hasNext());
		
		ZippedEntry<Integer, String> second = i.next();
		
		assertEquals(Integer.valueOf(1), second.getFirst());
		assertEquals("world", second.getSecond());
		
		assertFalse(i.hasNext());
	}
	
	@Test
	public void testIndex() {
		Iterator<IndexedEntry<String>> i =		
			index(Arrays.asList("Hello", "world").stream())
				.iterator();
		
		assertTrue(i.hasNext());
		
		IndexedEntry<String> first = i.next();
		assertNotNull(first);
		assertEquals(0, first.getIndex());
		assertEquals("Hello", first.getValue());
		
		assertTrue(i.hasNext());
		
		IndexedEntry<String> second = i.next();
		assertNotNull(second);
		assertEquals(1, second.getIndex());
		assertEquals("world", second.getValue());
		
		assertFalse(i.hasNext());
	}
	
	@Test
	public void testZipToMap() {
		Map<Integer, String> result = StreamUtils.zipToMap(
			Stream.of(1, 2, 3),
			Stream.of("a", "b", "c"));
		
		assertTrue(result.containsKey(1));
		assertEquals("a", result.get(1));
		
		assertTrue(result.containsKey(2));
		assertEquals("b", result.get(2));
		
		assertTrue(result.containsKey(3));
		assertEquals("c", result.get(3));
	}
	
	static class Pair<T, U> {
		
		final T t;
		
		final U u;		
		
		Pair(T t, U u) {
			this.t = t;
			this.u = u;
		}
	}
	
	@Test
	public void testWrap() {
		List<Pair<Integer, String>> list = Arrays.asList(
				new Pair<>(2, "two"),
				new Pair<>(1, "one (first)"),
				new Pair<>(4, "four"),
				new Pair<>(1, "one (second)"),
				new Pair<>(3, "three"));
		
		assertEquals(
			list.size(),
			StreamUtils.zip(
				list.stream()
					.map(p -> p.t)
					.sorted(),
					
				list.stream()	
					.map(StreamUtils.wrap(p -> p.t))
					.sorted()
					.map(StreamUtils.Wrapper::unwrap))
			
				.filter(entry -> entry.getFirst().equals(entry.getSecond().t))
				.count());
		
		assertEquals(
			list.size(),
			StreamUtils.zip(
				list.stream()
					.map(p -> p.u)
					.sorted(Comparator.<String>naturalOrder().reversed()),
					
				list.stream()	
					.map(StreamUtils.wrap(p -> p.u, Comparator.<String>naturalOrder().reversed()))
					.sorted()
					.map(StreamUtils.Wrapper::unwrap))
			
				.filter(entry -> entry.getFirst().equals(entry.getSecond().u))
				.count());
		
		assertEquals(
			list.size() - 1,
			list.stream()
				.map(p -> p.t)
				.distinct()
				.count());
		
		assertEquals(			
			list.size() - 1,
		
			list.stream()
				.map(StreamUtils.wrap(p -> p.t))
				.distinct()
				.map(StreamUtils.Wrapper::unwrap)
				.count());
	}
}
