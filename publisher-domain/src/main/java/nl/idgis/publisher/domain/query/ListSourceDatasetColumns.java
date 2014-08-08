package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class ListSourceDatasetColumns implements DomainQuery<List<Column>> {

	private static final long serialVersionUID = 7841056580822174674L;
	
	private final String dataSourceId, sourceDatasetId;
	
	public ListSourceDatasetColumns(String dataSourceId, String sourceDatasetId) {
		this.dataSourceId = dataSourceId;
		this.sourceDatasetId = sourceDatasetId;
	}
	
	public String getDataSourceId() {
		return dataSourceId;
	}
	
	public String getSourceDatasetId() {
		return sourceDatasetId;
	}
}
