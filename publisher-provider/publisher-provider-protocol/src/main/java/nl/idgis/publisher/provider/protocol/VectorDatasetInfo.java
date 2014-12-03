package nl.idgis.publisher.provider.protocol;

import java.util.Set;

import nl.idgis.publisher.domain.Log;

public class VectorDatasetInfo extends DatasetInfo {

	private static final long serialVersionUID = 8651322510331387757L;

	private final TableDescription tableDescription;
	
	private final long numberOfRecords;

	public VectorDatasetInfo(String identification, String title, Set<Attachment> attachments, 
		Set<Log> logs, TableDescription tableDescription, long numberOfRecords) {		
		super(identification, title, attachments, logs);
		
		this.tableDescription = tableDescription;
		this.numberOfRecords = numberOfRecords;
	}

	public TableDescription getTableDescription() {
		return tableDescription;
	}	
	
	public long getNumberOfRecords() {
		return numberOfRecords;
	}

	@Override
	public String toString() {
		return "VectorDatasetInfo [tableDescription=" + tableDescription
				+ ", numberOfRecords=" + numberOfRecords + ", identification="
				+ identification + ", title=" + title + "]";
	}

}
