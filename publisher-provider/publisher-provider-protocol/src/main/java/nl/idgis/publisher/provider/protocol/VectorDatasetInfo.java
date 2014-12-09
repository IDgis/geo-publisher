package nl.idgis.publisher.provider.protocol;

import java.util.Set;

import nl.idgis.publisher.domain.Log;

/**
 * A description of a vector dataset.
 * 
 * @author copierrj
 *
 */
public class VectorDatasetInfo extends DatasetInfo {

	private static final long serialVersionUID = 8651322510331387757L;

	private final TableDescription tableDescription;
	
	private final long numberOfRecords;

	/**
	 * 
	 * @param identification the identifier of the dataset.
	 * @param title the title of the dataset.
	 * @param attachments the attachments of the datasets.
	 * @param logs logs for the dataset.
	 * @param tableDescription table description of the dataset.
	 * @param numberOfRecords the number of records.
	 */
	public VectorDatasetInfo(String identification, String title, Set<Attachment> attachments, 
		Set<Log> logs, TableDescription tableDescription, long numberOfRecords) {		
		super(identification, title, attachments, logs);
		
		this.tableDescription = tableDescription;
		this.numberOfRecords = numberOfRecords;
	}

	/**
	 * 
	 * @return table description
	 */
	public TableDescription getTableDescription() {
		return tableDescription;
	}	
	
	/**
	 * 
	 * @return number of records
	 */
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
