package nl.idgis.publisher.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.Test;

public class GZIPSerializerTest {
	
	public static class TestClass implements Serializable {
		
		private static final GZIPSerializer<TestClass> serializer = new GZIPSerializer<>(TestClass.class);

		private static final long serialVersionUID = -6439138135964129848L;

		private final int i;
		
		private final transient String s;
		
		public TestClass(final int i, final String s) {
			this.i = i;
			this.s = s;
		}
		
		public int getInteger() {
			return i;
		}
		
		public String getString() {
			return s;
		}
		
		private void writeObject(ObjectOutputStream stream) throws IOException {
			serializer.write(stream, this);
		}
		
		private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
			serializer.read(stream, this);
		}
	}

	@Test
	public void testSerializer() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		
		oos.writeObject(new TestClass(42, "Hello, world!"));
		oos.close();
		
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		ObjectInputStream ois = new ObjectInputStream(bais);
		
		Object o = ois.readObject();
		assertTrue(o instanceof TestClass);
		
		TestClass testClass = (TestClass)o;
		assertEquals(42, testClass.getInteger());
		assertNull(testClass.getString());
	}
}
