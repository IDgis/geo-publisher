package nl.idgis.publisher.utils;

public class MathUtils {
	
	public static String toPrettyString(double value, int decimalPlaces) {
		long pow = 1;
		for(int i = 0; i < decimalPlaces; i++) {
			pow *= 10;
		}
		
		value = value * pow + 0.5;

		long longValue = (long)value;
		
		StringBuilder sb = new StringBuilder();
		sb.append(longValue / pow);
		
		if(decimalPlaces > 0) {
			String decimals = "" + longValue % pow;
			
			sb.append(".");
			sb.append(decimals);
			
			for(int i = decimals.length(); i < decimalPlaces; i++) {
				sb.append("0");
			}
		} 
		
		return sb.toString();
	}

	public static String toPrettySize(long value) {
		String[] prefixes = {"B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB"};
		
		int prefix;
		double result = value;
		for(prefix = 0; result >= 1024 && prefix < prefixes.length; prefix++) {
			result /= 1024;
		}
		
		if(prefix == 0) {
			return "" + value + prefixes[prefix];
		} else {
			return "" + toPrettyString(result, 2) + prefixes[prefix];
		}
	}
}
