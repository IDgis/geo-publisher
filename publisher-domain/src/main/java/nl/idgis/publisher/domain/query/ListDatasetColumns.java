package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class ListDatasetColumns implements DomainQuery<List<Column>> {

	private static final long serialVersionUID = -8688877263186058653L;
	
	private final String datasetId;
	
	public ListDatasetColumns(String datasetId) {
		this.datasetId = datasetId;
	}
	
	public String getDatasetId() {
		return datasetId;
	}
}
