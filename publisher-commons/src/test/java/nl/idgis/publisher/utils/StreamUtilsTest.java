package nl.idgis.publisher.utils;

import java.util.Arrays;
import java.util.Iterator;

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
}
