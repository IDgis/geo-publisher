package nl.idgis.publisher.harvester.sources;

public class ProviderUtils {

	public static String getTableName(String alternateTitle) {
		if(alternateTitle != null 
			&& !alternateTitle.trim().isEmpty()) {			 
		
			final String tableName;
			if(alternateTitle.contains(" ")) {
				tableName = alternateTitle.substring(0, alternateTitle.indexOf(" ")).trim();
			} else {
				tableName = alternateTitle.trim();
			}
			
			return tableName.toUpperCase();
		}
		
		return null;
	}
	
	public static String getCategoryId(String alternateTitle) {
		String tableName = getTableName(alternateTitle);
		
		if(tableName != null) {
			int separator = tableName.indexOf(".");
			if(separator != -1) {
				String schemaName = tableName.substring(0, separator);
				return schemaName;
			}
		}
		
		return null;
	}
}
