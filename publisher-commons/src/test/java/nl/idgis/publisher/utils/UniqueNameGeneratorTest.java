package nl.idgis.publisher.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class UniqueNameGeneratorTest {

	@Test
	public void testBaseName() {
		UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
		
		assertTrue(nameGenerator.baseNameCount.isEmpty());
		
		assertEquals("test-name-0", nameGenerator.getName("test-name"));
		assertEquals("test-name-1", nameGenerator.getName("test-name"));
		assertEquals("test-name-2", nameGenerator.getName("test-name"));
		
		assertEquals(1, nameGenerator.baseNameCount.size());
		assertTrue(nameGenerator.baseNameCount.containsKey("test-name"));
		
		assertEquals("another-test-name-0", nameGenerator.getName("another-test-name"));
		assertEquals("another-test-name-1", nameGenerator.getName("another-test-name"));
		
		assertEquals(2, nameGenerator.baseNameCount.size());
		assertTrue(nameGenerator.baseNameCount.containsKey("test-name"));
		assertTrue(nameGenerator.baseNameCount.containsKey("another-test-name"));
	}
	
	@Test
	public void testClasses() {
		UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
		
		assertTrue(nameGenerator.baseNames.isEmpty());
		assertTrue(nameGenerator.baseNameCount.isEmpty());
		
		assertEquals("unique-name-generator-unique-name-generator-test-0", nameGenerator.getName(UniqueNameGenerator.class, this.getClass()));
		assertEquals("unique-name-generator-unique-name-generator-test-1", nameGenerator.getName(UniqueNameGenerator.class, this.getClass()));
		assertEquals("unique-name-generator-unique-name-generator-test-2", nameGenerator.getName(UniqueNameGenerator.class, this.getClass()));
		
		assertEquals(1, nameGenerator.baseNameCount.size());
		assertTrue(nameGenerator.baseNameCount.containsKey("unique-name-generator-unique-name-generator-test"));
		
		assertEquals(1, nameGenerator.baseNames.size());
		assertTrue(nameGenerator.baseNames.containsKey(Arrays.asList(UniqueNameGenerator.class, this.getClass())));
	}
}
