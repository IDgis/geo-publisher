package nl.idgis.publisher.domain.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.service.Column;

public class PutDataset extends Identifiable {

	private static final long serialVersionUID = -3157012350028991268L;
	
	private final String datasetIdentification;
	private final String sourceDatasetId;
	private final List<Column> columnList;
	
	public PutDataset(final @JsonProperty("id") String id, final @JsonProperty("name") String datasetIdentification,
			final @JsonProperty("sourceDataset") String sourceDatasetId, final @JsonProperty("columnList") List<Column> columnList) {
		super(id);
		this.datasetIdentification = datasetIdentification;
		this.sourceDatasetId = sourceDatasetId;
		this.columnList = columnList;
	}

	public String getDatasetIdentification() {
		return datasetIdentification;
	}

	public String getSourceDataset() {
		return sourceDatasetId;
	}

	public List<Column> getColumnList() {
		return columnList;
	}

	@Override
	public String toString() {
		return "PutDataset [datasetIdentification=" + datasetIdentification
				+ ", sourceDataset=" + sourceDatasetId + ", columnList="
				+ columnList + "]";
	}
	

}
