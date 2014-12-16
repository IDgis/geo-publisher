package nl.idgis.publisher.provider;

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
			
			return tableName.replace(":", ".").toLowerCase();
		}
		
		return null;
	}
	
	public static String getCategoryId(String tableName) {
		if(tableName != null) {
			int separator = tableName.indexOf(".");
			if(separator != -1) {
				String schemaName = tableName.substring(0, separator);
				return schemaName.toLowerCase();
			}
		}
		
		return null;
	}
}
