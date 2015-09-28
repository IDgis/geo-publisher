package nl.idgis.publisher.utils;

import static nl.idgis.publisher.utils.StreamUtils.index;
import static nl.idgis.publisher.utils.StreamUtils.zip;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import nl.idgis.publisher.utils.StreamUtils.IndexedEntry;
import nl.idgis.publisher.utils.StreamUtils.ZippedEntry;

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
		
		public T getT() {
			return t;
		}
		
		public U getU() {
			return u;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((t == null) ? 0 : t.hashCode());
			result = prime * result + ((u == null) ? 0 : u.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Pair<?, ?> other = (Pair<?, ?>) obj;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			if (u == null) {
				if (other.u != null)
					return false;
			} else if (!u.equals(other.u))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Pair [t=" + t + ", u=" + u + "]";
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
	
	@Test
	public void testMergeJoin() {
		Stream<Pair<List<Pair<Integer, String>>, List<Pair<Integer, String>>>> result =		
			StreamUtils.mergeJoin(
				Stream.of(
					new Pair<>(1, "one"),
					new Pair<>(2, "two"),
					new Pair<>(3, "three"),
					new Pair<>(5, "five")), 
				Stream.of(
					new Pair<>(1, "a"),
					new Pair<>(1, "b"),
					new Pair<>(1, "c"),
					new Pair<>(3, "d"),
					new Pair<>(3, "e"),
					new Pair<>(4, "f"),
					new Pair<>(4, "g"),
					new Pair<>(5, "h"),
					new Pair<>(5, "i")),
				(first, second) -> first.t - second.t,
				Collectors.toList(),
				Collectors.toList(),
				(firstList, secondList) -> {
					return new Pair<List<Pair<Integer, String>>, List<Pair<Integer, String>>>(firstList, secondList);
				});
		
		assertNotNull(result);
		
		Iterator<Pair<List<Pair<Integer, String>>, List<Pair<Integer, String>>>> actual = result.iterator();
		
		Iterator<Pair<List<Pair<Integer, String>>, List<Pair<Integer, String>>>> expected = 
			Arrays
				.asList(
					new Pair<>(
						Arrays.asList(
							new Pair<>(1, "one")),
						Arrays.asList(
							new Pair<>(1, "a"),
							new Pair<>(1, "b"),
							new Pair<>(1, "c"))),
					new Pair<>(
						Arrays.asList(
							new Pair<>(2, "two")),
						Collections.<Pair<Integer, String>>emptyList()),
					new Pair<>(
						Arrays.asList(
							new Pair<>(3, "three")),
						Arrays.asList(
							new Pair<>(3, "d"),
							new Pair<>(3, "e"))),
					new Pair<>(
						Collections.<Pair<Integer, String>>emptyList(),
						Arrays.asList(
							new Pair<>(4, "f"))),
					new Pair<>(
						Collections.<Pair<Integer, String>>emptyList(),
						Arrays.asList(
							new Pair<>(4, "g"))),
					new Pair<>(
						Arrays.asList(
							new Pair<>(5, "five")),
						Arrays.asList(
							new Pair<>(5, "h"),
							new Pair<>(5, "i"))))
				.iterator();
		
		while(expected.hasNext()) {
			assertTrue(actual.hasNext());
			assertEquals(expected.next(), actual.next());
		}
	}
	
	@Test(expected=NoSuchElementException.class)
	public void testMergeJoinBothEmpty() {
		Iterator<String> itr = StreamUtils.mergeJoin(
			Stream.empty(), 
			Stream.empty(), 
			(t, u) -> 0,
			Collectors.toList(),
			Collectors.toList(),
			(tv, uv) -> "Hello, world!").iterator();
		
		assertFalse(itr.hasNext());
		itr.next();
	}
	
	@Test(expected=NoSuchElementException.class)
	public void testMergeJoinLeftEmpty() {
		Iterator<Pair<List<String>, List<Object>>> itr = StreamUtils
			.mergeJoin(
				Stream.of("Hello, world!"), 
				Stream.empty(), 
				(t, u) -> 0,
				Collectors.toList(),
				Collectors.toList(),
				(tv, uv) -> new Pair<>(tv, uv))
			.iterator();
			
		assertTrue(itr.hasNext());
		assertEquals(new Pair<>(Arrays.asList("Hello, world!"), Collections.emptyList()), itr.next());
		assertFalse(itr.hasNext());
		itr.next();
	}

	@Test(expected=NoSuchElementException.class)
	public void testMergeJoinRightEmpty() {
		Iterator<Pair<List<Object>, List<String>>> itr = StreamUtils
			.mergeJoin(
				Stream.empty(),
				Stream.of("Hello, world!"),
				(t, u) -> 0,
				Collectors.toList(),
				Collectors.toList(),
				(tv, uv) -> new Pair<>(tv, uv))
			.iterator();
			
		assertTrue(itr.hasNext());
		assertEquals(new Pair<>(Collections.emptyList(), Arrays.asList("Hello, world!")), itr.next());
		assertFalse(itr.hasNext());
		itr.next();
	}
	
	@Test
	public void testPartition() {
		Iterator<String> actual = 
			StreamUtils.partition(Stream.<Pair<Integer, String>>of(
					new Pair<>(0, "a"),
					new Pair<>(0, "b"),
					new Pair<>(1, "c"),
					new Pair<>(2, "d"),
					new Pair<>(2, "e"),
					new Pair<>(2, "f"),
					new Pair<>(3, null),
					new Pair<>(4, "g"),
					new Pair<>(4, "h")), Pair::getT, Collectors.toList())
				.map(partition -> 
					partition.key() + ": "
						+ partition.stream()
							.map(Pair::getU)
							.filter(u -> u != null)
							.collect(Collectors.joining(",")))
				.iterator();
		
		Iterator<String> expected = Stream.of("0: a,b", "1: c", "2: d,e,f", "3: ", "4: g,h").iterator();
		
		while(expected.hasNext()) {
			assertTrue(actual.hasNext());
			assertEquals(expected.next(), actual.next());
		}
	}
}
