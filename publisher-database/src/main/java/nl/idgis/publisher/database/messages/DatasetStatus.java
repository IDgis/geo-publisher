package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class DatasetStatus implements Serializable {
	
	private static final long serialVersionUID = 2618280080822609672L;
	
	private final String datasetId;
	private final Timestamp sourceRevision, importedRevision;	
	private final List<Column> columns, sourceColumns, importedColumns;
	
	public DatasetStatus(String datasetId, Timestamp sourceRevision, Timestamp importedRevision, List<Column> columns, List<Column> sourceColumns, List<Column> importedColumns) {
		this.datasetId = datasetId;
		this.sourceRevision = sourceRevision;
		this.importedRevision = importedRevision;
		this.columns = columns;
		this.sourceColumns = sourceColumns;
		this.importedColumns = importedColumns;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public Timestamp getSourceRevision() {
		return sourceRevision;
	}

	public Timestamp getImportedRevision() {
		return importedRevision;
	}
	
	public List<Column> getColumns() {
		return columns;
	}

	public List<Column> getSourceColumns() {
		return sourceColumns;
	}

	public List<Column> getImportedColumns() {
		return importedColumns;
	}

	@Override
	public String toString() {
		return "DatasetStatus [datasetId=" + datasetId + ", sourceRevision="
				+ sourceRevision + ", importedRevision=" + importedRevision
				+ ", columns=" + columns + ", sourceColumns=" + sourceColumns
				+ ", importedColumns=" + importedColumns + "]";
	}
	
}
