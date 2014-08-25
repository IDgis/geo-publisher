package nl.idgis.publisher.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;

public class TypedIterableTest {

	@Test
	public void testContains() {
		TypedIterable<?> ti = new TypedIterable<>(Integer.class, Arrays.asList(42, 47));
		assertTrue(ti.contains(Integer.class));
		assertTrue(ti.contains(Number.class));
	}
	
	@Test
	public void testCast() {
		TypedIterable<?> ti = new TypedIterable<>(Integer.class, Arrays.asList(42, 47));
		
		Iterator<Integer> i = ti.cast(Integer.class).iterator();
		assertEquals(42, i.next().intValue());
		assertEquals(47, i.next().intValue());
		
		Iterator<Number> n = ti.cast(Number.class).iterator();
		assertEquals(42, n.next().intValue());
		assertEquals(47, n.next().intValue());
	}
}
