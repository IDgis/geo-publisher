package nl.idgis.publisher.domain.log;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class Events {
	
	private final static BiMap<String, Class<? extends Enum<?>>> prefixes = prefixes();
	
	private static BiMap<String, Class<? extends Enum<?>>> prefixes() {
		BiMap<String, Class<? extends Enum<?>>> prefixes = HashBiMap.create();
		prefixes.put("GENERIC", GenericEvent.class);
		prefixes.put("HARVEST", HarvestEvent.class);
		
		return prefixes;
	}

	@SuppressWarnings("unchecked")
	public static Enum<?> toEvent(String event) {
		String[] parts = event.split("\\.");
		
		Class<?> enumClass = prefixes.get(parts[0]);		
		return Enum.valueOf(enumClass.asSubclass(Enum.class), parts[1]);
	}
	
	public static String toString(Enum<?> event) {
		BiMap<Class<? extends Enum<?>>, String> inversePrefixes = prefixes.inverse();
		return inversePrefixes.get(event.getClass()) + "." + event.name();
	}
}
