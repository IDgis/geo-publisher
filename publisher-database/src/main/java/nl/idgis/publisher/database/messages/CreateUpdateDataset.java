package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class CreateUpdateDataset extends Query {

	private static final long serialVersionUID = -7841672424518750710L;

	private final String datasetIdentification;
	private final String datasetName;
	private final String sourceDatasetIdentification;
	private final List<Column> columnList;
	private final String filterConditions;
	
	public CreateUpdateDataset(String datasetIdentification, String datasetName, String sourceDatasetIdentification,
			List<Column> columnList, final String filterConditions) {
		super();
		this.datasetIdentification = datasetIdentification;
		this.datasetName = datasetName;
		this.sourceDatasetIdentification = sourceDatasetIdentification;
		this.columnList = columnList;
		this.filterConditions = filterConditions;
	}
	
	public String getDatasetIdentification() {
		return datasetIdentification;
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
	
	public String getFilterConditions () {
		return filterConditions;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + " [datasetId=" + datasetIdentification
				+ ", sourceDatasetId=" + sourceDatasetIdentification + ", columnList="
				+ columnList + "]";
	}

}
