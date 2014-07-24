package nl.idgis.publisher.harvester.sources;

import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;

public class ProviderUtils {

	public static String getTableName(MetadataItem metadataItem) {
		String alternateTitle = metadataItem.getAlternateTitle();
		
		if(alternateTitle != null 
			&& !alternateTitle.trim().isEmpty()) {			 
		
			if(alternateTitle.contains(" ")) {
				return alternateTitle.substring(0, alternateTitle.indexOf(" ")).trim();
			} else {
				return alternateTitle.trim();
			}
		}
		
		return null;
	}
}
