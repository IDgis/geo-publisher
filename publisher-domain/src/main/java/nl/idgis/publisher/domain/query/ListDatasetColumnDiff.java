package nl.idgis.publisher.domain.query;

import java.util.List;

import nl.idgis.publisher.domain.service.ColumnDiff;

public class ListDatasetColumnDiff implements DomainQuery<List<ColumnDiff>> {
	private static final long serialVersionUID = 7267357274291817586L;
	
	private final String datasetIdentification;
	
	public ListDatasetColumnDiff (final String datasetIdentification) {
		if (datasetIdentification == null) {
			throw new NullPointerException ("datasetIdentification cannot be null");
		}
		
		this.datasetIdentification = datasetIdentification;
	}
	
	public String datasetIdentification () {
		return datasetIdentification;
	}
}
