package nl.idgis.publisher.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Test;

public class LazyTest {

	@Test
	public void testGet() {
		assertEquals(Integer.valueOf(42), new Lazy<Integer>(() -> 42).get());
	}
	
	@Test
	public void testSupplyOnce() {
		Lazy<Integer> lazyValue = new Lazy<>(new Lazy.LazySupplier<Integer>() {			
			
			private static final long serialVersionUID = -5183996451704320273L;
			
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
	
	static class TestSerializable implements Serializable {		
		
		private static final long serialVersionUID = -9029456671476202582L;
		
		Lazy<Integer> lazyValue;
		
		TestSerializable(Lazy<Integer> lazyValue) {
			this.lazyValue = lazyValue;
		}
	}
	
	@Test	
	public void testSerializable() throws Exception {
		Lazy<Integer> lazyValue = new Lazy<Integer>(() -> 42);
		lazyValue.get();
		
		TestSerializable testSerializable = new TestSerializable(lazyValue);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		
		oos.writeObject(testSerializable);		
		oos.close();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		
		Object obj = ois.readObject();
		assertTrue(obj instanceof TestSerializable);
		
		Lazy<Integer> deserializedLazyValue = ((TestSerializable)obj).lazyValue;
		assertEquals(Integer.valueOf(42), deserializedLazyValue.get());
	}
}
