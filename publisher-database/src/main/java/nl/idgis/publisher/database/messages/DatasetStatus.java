package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class DatasetStatus implements Serializable {

	private static final long serialVersionUID = -3314499936419517295L;
	
	private final String datasetId;
	private final Timestamp revision, importedRevision;	
	private final List<Column> columns, importedColumns;
	
	public DatasetStatus(String datasetId, Timestamp revision, Timestamp importedRevision, List<Column> columns, List<Column> importedColumns) {
		this.datasetId = datasetId;
		this.revision = revision;
		this.importedRevision = importedRevision;
		this.columns = columns;
		this.importedColumns = importedColumns;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public Timestamp getRevision() {
		return revision;
	}

	public Timestamp getImportedRevision() {
		return importedRevision;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public List<Column> getImportedColumns() {
		return importedColumns;
	}

	@Override
	public String toString() {
		return "DatasetStatus [datasetId=" + datasetId + ", revision="
				+ revision + ", importedRevision=" + importedRevision
				+ ", columns=" + columns + ", importedColumns="
				+ importedColumns + "]";
	}
}
