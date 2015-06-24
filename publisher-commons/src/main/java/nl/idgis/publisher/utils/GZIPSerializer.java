package nl.idgis.publisher.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GZIPSerializer<T> {
	
	private final Class<T> clazz;
	
	private final List<Field> fields;
	
	private static class GZIP implements Serializable {		

		private static final long serialVersionUID = -7978408016842285933L;
		
		private final byte[] b;
		
		GZIP(byte[] payload) {
			this.b = Objects.requireNonNull(payload);
		}
		
		public byte[] getPayload() {
			return b;
		}
	}

	public GZIPSerializer(Class<T> clazz) {
		this.clazz = clazz;
		
		fields = Arrays.asList(clazz.getDeclaredFields()).stream()
			.filter(field -> 
				(field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) == 0
				|| "serialVersionUID".equals(field.getName()))
			.collect(Collectors.toList());
	}
	
	public void read(ObjectInputStream stream, T t) throws IOException, ClassNotFoundException {
		Object obj = stream.readObject();
		if(obj instanceof GZIP) {
			ByteArrayInputStream bais = new ByteArrayInputStream(((GZIP)obj).getPayload());
			GZIPInputStream gis = new GZIPInputStream(bais);
			ObjectInputStream ois = new ObjectInputStream(gis);
			
			int fieldCount = ois.readInt();
			for(int i = 0; i < fieldCount; i++) {			
				Object fieldName = ois.readObject();
				if(fieldName instanceof String) {
					Object fieldValue = ois.readObject();
					
					try {
						Field field = clazz.getDeclaredField((String)fieldName);
						field.setAccessible(true);
						if((field.getModifiers() & Modifier.STATIC) > 0) {							
							if(!field.get(null).equals(fieldValue)) {
								throw new IOException("wrong value for field: " + fieldName);
							}
						} else {							
							field.set(t, fieldValue);
						}
					} catch(Exception e) {
						throw new IOException("couldn't set field", e);
					}
				} else {
					throw new IOException("String expected");
				}
			}
		} else {
			throw new IOException("GZIP expected");
		}
	}
	
	public void write(ObjectOutputStream stream, T t) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gos = new GZIPOutputStream(baos);
		ObjectOutputStream oos = new ObjectOutputStream(gos);
		
		oos.writeInt(fields.size());		
		for(Field field : fields) {
			oos.writeObject(field.getName());
			
			try {
				field.setAccessible(true);
				oos.writeObject(field.get(t));
			} catch(Exception e) {
				throw new IOException("couldn't read field", e);
			}
		}
		
		oos.close();
		stream.writeObject(new GZIP(baos.toByteArray()));
	}
}
