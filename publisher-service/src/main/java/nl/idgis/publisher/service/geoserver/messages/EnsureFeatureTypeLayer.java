package nl.idgis.publisher.service.geoserver.messages;

import nl.idgis.publisher.service.geoserver.rest.FeatureType;

public class EnsureFeatureTypeLayer extends EnsureLayer {	

	private static final long serialVersionUID = 4910082363406762765L;
	
	private final String tableName;
	
	public EnsureFeatureTypeLayer(String layerId, String title, String abstr, String tableName) {
		super(layerId, title, abstr);
		
		this.tableName = tableName;
	}
	
	public String getTableName() {
		return tableName;
	}
	
	public FeatureType getFeatureType() {
		return new FeatureType(
				layerId, 
				tableName,
				title,
				abstr);
	}
	
}
