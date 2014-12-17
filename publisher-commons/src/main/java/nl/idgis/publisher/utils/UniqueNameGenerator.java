package nl.idgis.publisher.utils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UniqueNameGenerator {
	
	protected final Map<List<Class<?>>, String> baseNames = new HashMap<>();
	protected final Map<String, BigInteger> baseNameCount = new HashMap<>();
	
	public String getName(Class<?>... classes) {
		return getName(getBaseName(classes));
	}

	protected String getBaseName(Class<?>... classes) {
		List<Class<?>> classesList = Arrays.asList(classes);
		
		if(baseNames.containsKey(classesList)) {
			return baseNames.get(classesList);
		} else {
			StringBuilder sb = new StringBuilder();
			
			String separator = "";
			for(Class<?> clazz : classes) {
				sb.append(separator);
				sb.append(getClassName(clazz));
				
				separator = "-";
			}
			
			String baseName = sb.toString();
			baseNames.put(classesList, baseName);
			
			return baseName;
		}
	}

	protected CharSequence getClassName(Class<?> clazz) {
		StringBuilder sb = new StringBuilder();
		
		String simpleName = clazz.getSimpleName();
		for(int i = 0; i < simpleName.length(); i++) {
			char c = simpleName.charAt(i);
			if(Character.isUpperCase(c)) {
				if(i > 0) {
					sb.append("-");
				}
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		
		return sb;
	}

	public String getName(String baseName) {
		
		final BigInteger currentCount;		
		if(baseNameCount.containsKey(baseName)) {
			currentCount = baseNameCount.get(baseName).add(BigInteger.valueOf(1));
		} else {
			currentCount = BigInteger.valueOf(0);
		}
		
		baseNameCount.put(baseName, currentCount);
		
		return baseName + "-" + currentCount.toString(Character.MAX_RADIX);
	}
}