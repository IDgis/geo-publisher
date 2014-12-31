package nl.idgis.publisher.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

public class TypedListTest {

	@Test
	public void testEquals() {
		TypedList<String> t0 = new TypedList<>(String.class, Arrays.asList("a", "b"));
		TypedList<String> t1 = new TypedList<>(String.class, Arrays.asList("a", "b"));
		
		assertEquals(t0, t1);
	}
}
