package nl.idgis.publisher.provider.protocol;

import java.util.Set;

public class VectorDatasetInfo extends DatasetInfo {
	
	private static final long serialVersionUID = -7886791420320324471L;

	private final TableDescription tableDescription;
	
	private final long numberOfRecords;

	public VectorDatasetInfo(String id, String title, Set<Attachment> attachments, 
		Set<Message<?>> messages, TableDescription tableDescription, long numberOfRecords) {		
		super(id, title, attachments, messages);
		
		this.tableDescription = tableDescription;
		this.numberOfRecords = numberOfRecords;
	}

	public TableDescription getTableDescription() {
		return tableDescription;
	}	
	
	public long getNumberOfRecords() {
		return numberOfRecords;
	}

}
