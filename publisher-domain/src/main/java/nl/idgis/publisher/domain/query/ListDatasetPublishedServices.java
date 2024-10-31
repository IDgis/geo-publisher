package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.web.DatasetPublishedService;

public class ListDatasetPublishedServices implements DomainQuery<List<DatasetPublishedService>> {

	private static final long serialVersionUID = 1L;
	
	private final String datasetId;
	
	public ListDatasetPublishedServices (final String datasetId) {
		this.datasetId = datasetId;
	}
	
	public String getDatasetId () {
		return this.datasetId;
	}
}
