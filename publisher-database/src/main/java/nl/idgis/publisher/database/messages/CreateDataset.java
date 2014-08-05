package nl.idgis.publisher.database.messages;

import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class CreateDataset extends Query {

	private static final long serialVersionUID = -7841672424518750710L;

	private final String datasetIdentification;
	private final String sourceDatasetIdentification;
	private final List<Column> columnList;
	
	public CreateDataset(String datasetIdentification, String sourceDatasetIdentification,
			List<Column> columnList) {
		super();
		this.datasetIdentification = datasetIdentification;
		this.sourceDatasetIdentification = sourceDatasetIdentification;
		this.columnList = columnList;
	}
	
	public String getDatasetIdentification() {
		return datasetIdentification;
	}
	public String getSourceDatasetIdentification() {
		return sourceDatasetIdentification;
	}
	public List<Column> getColumnList() {
		return columnList;
	}

	@Override
	public String toString() {
		return "CreateDataset [datasetId=" + datasetIdentification
				+ ", sourceDatasetId=" + sourceDatasetIdentification + ", columnList="
				+ columnList + "]";
	}

}
