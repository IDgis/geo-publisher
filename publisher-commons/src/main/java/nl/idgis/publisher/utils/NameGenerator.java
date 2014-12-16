package nl.idgis.publisher.utils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class NameGenerator {
	
	private final Map<String, BigInteger> baseNames = new HashMap<String, BigInteger>();

	public String getName(String baseName) {
		
		final BigInteger currentCount;		
		if(baseNames.containsKey(baseName)) {
			currentCount = baseNames.get(baseName).add(BigInteger.valueOf(1));
		} else {
			currentCount = BigInteger.valueOf(0);
		}
		
		baseNames.put(baseName, currentCount);
		
		return baseName + "-" + currentCount.toString(Character.MAX_RADIX);
	}
}
