package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class DatasetStatus implements Serializable {
	
	private static final long serialVersionUID = -1676229472913952243L;
	
	private final String datasetId;
	private final Timestamp sourceRevision, importedSourceRevision;	
	private final List<Column> columns, importedColumns, sourceColumns, importedSourceColumns;
	
	public DatasetStatus(
			String datasetId, 
			
			Timestamp sourceRevision, 
			Timestamp importedSourceRevision, 
			
			List<Column> columns, 
			List<Column> importedColumns, 
			List<Column> sourceColumns, 
			List<Column> importedSourceColumns) {
		
		this.datasetId = datasetId;
		
		this.sourceRevision = sourceRevision;
		this.importedSourceRevision = importedSourceRevision;
		
		this.columns = columns;
		this.importedColumns = importedColumns; 
		this.sourceColumns = sourceColumns;
		this.importedSourceColumns = importedSourceColumns;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public Timestamp getSourceRevision() {
		return sourceRevision;
	}

	public Timestamp getImportedSourceRevision() {
		return importedSourceRevision;
	}
	
	public List<Column> getColumns() {
		return columns;
	}
	
	public List<Column> getImportedColumns() {
		return importedColumns;
	}

	public List<Column> getSourceColumns() {
		return sourceColumns;
	}

	public List<Column> getImportedSourceColumns() {
		return importedSourceColumns;
	}

	@Override
	public String toString() {
		return "DatasetStatus [datasetId=" + datasetId + ", sourceRevision="
				+ sourceRevision + ", importedSourceRevision="
				+ importedSourceRevision + ", columns=" + columns
				+ ", sourceColumns=" + sourceColumns
				+ ", importedSourceColumns=" + importedSourceColumns + "]";
	}

	
}
