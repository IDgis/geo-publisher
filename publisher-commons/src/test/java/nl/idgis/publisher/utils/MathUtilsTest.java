package nl.idgis.publisher.utils;

import static nl.idgis.publisher.utils.MathUtils.toPrettyString;
import static nl.idgis.publisher.utils.MathUtils.toPrettySize;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MathUtilsTest {
	
	@Test
	public void testToPrettyString() {
		assertEquals("42", toPrettyString(42.0, 0));
		assertEquals("42.00", toPrettyString(42.0, 2));
		
		assertEquals("42", toPrettyString(42.4, 0));
		assertEquals("42", toPrettyString(41.5, 0));
		
		assertEquals("42.1", toPrettyString(42.05, 1));
		assertEquals("42.0", toPrettyString(42.04, 1));
	}

	@Test
	public void testToPrettySize() {
		assertEquals("0B", toPrettySize(0));
		assertEquals("1023B", toPrettySize(1023));
		assertEquals("1.00KiB", toPrettySize(1024));
		assertEquals("1.50KiB", toPrettySize(1536));
		assertEquals("1.00MiB", toPrettySize(1024 * 1024));
	}
}
