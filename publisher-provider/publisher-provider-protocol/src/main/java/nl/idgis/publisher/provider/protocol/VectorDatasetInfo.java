package nl.idgis.publisher.provider.protocol;

import java.util.Set;

public class VectorDatasetInfo extends DatasetInfo {
	
	private static final long serialVersionUID = 6438612494365521637L;

	private final TableDescription tableDescription;
	
	private final long numberOfRecords;

	public VectorDatasetInfo(String id, String title, Set<Attachment> attachments, TableDescription tableDescription, long numberOfRecords) {
		super(id, title, attachments);
		
		this.tableDescription = tableDescription;
		this.numberOfRecords = numberOfRecords;
	}

	public TableDescription getTableDescription() {
		return tableDescription;
	}

	@Override
	public String toString() {
		return "VectorDatasetInfo [tableDescription=" + tableDescription
				+ ", numberOfRecords=" + numberOfRecords + ", id=" + id
				+ ", title=" + title + ", attachments=" + attachments + "]";
	}

}
