package nl.idgis.publisher.service.geoserver.messages;

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
	
}
