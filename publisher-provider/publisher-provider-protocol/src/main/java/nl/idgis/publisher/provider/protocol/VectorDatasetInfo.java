package nl.idgis.publisher.provider.protocol;

import java.util.Set;

public class VectorDatasetInfo extends DatasetInfo {

	private static final long serialVersionUID = 8651322510331387757L;

	private final TableDescription tableDescription;
	
	private final long numberOfRecords;

	public VectorDatasetInfo(String identification, String title, Set<Attachment> attachments, 
		Set<Message<?>> messages, TableDescription tableDescription, long numberOfRecords) {		
		super(identification, title, attachments, messages);
		
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
