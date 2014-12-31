package nl.idgis.publisher.domain;

public final class MessageTypeUtils {

	private MessageTypeUtils() {
		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends MessageType<?>> T valueOf(Class<T> clazz, String name) {
		if(Enum.class.isAssignableFrom(clazz)) {
			return (T)Enum.valueOf((Class<Enum>)clazz.asSubclass(Enum.class), name);
		}
		
		try {
			return clazz.getConstructor(String.class).newInstance(name);
		} catch(Exception e) {
			throw new IllegalArgumentException("provided class should be an Enum or should have a public constructor with a String parameter");
		}
	}
}
