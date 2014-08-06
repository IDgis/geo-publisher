package nl.idgis.publisher.domain.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.idgis.publisher.domain.query.DomainQuery;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.service.CrudOperation;

/**
 * PutDataset combines data from dataset and datasetcolumn.
 * @author Rob
 *
 */
public class PutDataset extends Identifiable {

	private static final long serialVersionUID = -3157012350028991268L;
	
	private final CrudOperation operation;
	private final String datasetName;
	private final String sourceDatasetIdentification;
	private final List<Column> columnList;
	
	public PutDataset(final @JsonProperty("operation") CrudOperation operation, final @JsonProperty("id") String datasetIdentification, final @JsonProperty("name") String datasetName,
			final @JsonProperty("sourceDataset") String sourceDatasetIdentification, final @JsonProperty("columnList") List<Column> columnList) {
		super(datasetIdentification);
		this.operation = operation;
		this.datasetName = datasetName;
		this.sourceDatasetIdentification = sourceDatasetIdentification;
		this.columnList = columnList;
	}

	public CrudOperation getOperation() {
		return operation;
	}
	
	public String getDatasetName() {
		return datasetName;
	}

	public String getSourceDatasetIdentification() {
		return sourceDatasetIdentification;
	}

	public List<Column> getColumnList() {
		return columnList;
	}

	@Override
	public String toString() {
		return "PutDataset [operation=" + operation + ", datasetName="
				+ datasetName + ", sourceDatasetIdentification="
				+ sourceDatasetIdentification + ", columnList=" + columnList
				+ "]";
	}

}
