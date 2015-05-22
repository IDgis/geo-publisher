package nl.idgis.publisher.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.function.Supplier;

import org.junit.Test;

public class LazyTest {

	@Test
	public void testGet() {
		assertEquals(Integer.valueOf(42), new Lazy<Integer>(() -> 42).get());
	}
	
	@Test
	public void testSupplyOnce() {
		Lazy<Integer> lazyValue = new Lazy<>(new Supplier<Integer>() {
			
			private boolean supplied = false;
			
			public Integer get() {
				assertFalse(supplied);
				
				supplied = true;
				
				return 42;
			}
		});
		
		assertEquals(Integer.valueOf(42), lazyValue.get());
		assertEquals(Integer.valueOf(42), lazyValue.get());
	}
}
