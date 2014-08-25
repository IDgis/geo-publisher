package nl.idgis.publisher.database.messages;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

import nl.idgis.publisher.domain.service.Column;

public class DatasetStatus implements Serializable {	
	
	private static final long serialVersionUID = -7669936512939852048L;
	
	private final String datasetId, sourceDatasetId, importedSourceDatasetId;
	private final Timestamp sourceRevision, importedSourceRevision;	
	private final List<Column> columns, importedColumns, sourceColumns, importedSourceColumns;
	private final boolean serviceCreated;
	
	public DatasetStatus(
			String datasetId, 
			
			String sourceDatasetId,
			String importedSourceDatasetId,
			
			Timestamp sourceRevision, 
			Timestamp importedSourceRevision,
			
			List<Column> columns, 
			List<Column> importedColumns, 
			List<Column> sourceColumns, 
			List<Column> importedSourceColumns,
			
			boolean serviceCreated) {
		
		this.datasetId = datasetId;
		
		this.sourceDatasetId = sourceDatasetId;
		this.importedSourceDatasetId = importedSourceDatasetId;
		
		this.sourceRevision = sourceRevision;
		this.importedSourceRevision = importedSourceRevision;
		
		this.columns = columns;
		this.importedColumns = importedColumns; 
		this.sourceColumns = sourceColumns;
		this.importedSourceColumns = importedSourceColumns;
		
		this.serviceCreated = serviceCreated;
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

	public String getSourceDatasetId() {
		return sourceDatasetId;
	}

	public String getImportedSourceDatasetId() {
		return importedSourceDatasetId;
	}
	
	public boolean isServiceCreated() {
		return serviceCreated;
	}

	@Override
	public String toString() {
		return "DatasetStatus [datasetId=" + datasetId + ", sourceDatasetId="
				+ sourceDatasetId + ", importedSourceDatasetId="
				+ importedSourceDatasetId + ", sourceRevision="
				+ sourceRevision + ", importedSourceRevision="
				+ importedSourceRevision + ", columns=" + columns
				+ ", importedColumns=" + importedColumns + ", sourceColumns="
				+ sourceColumns + ", importedSourceColumns="
				+ importedSourceColumns + ", serviceCreated=" + serviceCreated
				+ "]";
	}

	
	
}
