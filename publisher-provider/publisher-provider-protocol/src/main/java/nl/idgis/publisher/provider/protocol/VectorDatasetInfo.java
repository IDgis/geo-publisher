package nl.idgis.publisher.provider.protocol;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

/**
 * A description of a vector dataset.
 * 
 * @author copierrj
 *
 */
public class VectorDatasetInfo extends DatasetInfo {
	
	private static final long serialVersionUID = 1764174549959240190L;

	private final String tableName;

	private final TableInfo tableInfo;
	
	private final long numberOfRecords;

	/**
	 * 
	 * @param identification the identifier of the dataset.
	 * @param title the title of the dataset.
	 * @param alternateTitle the alternate title of the dataset.
	 * @param categoryId the identifier of the category for this dataset
	 * @param revisionDate the revision date of this dataset
	 * @param attachments the attachments of the datasets.
	 * @param logs logs for the dataset.
	 * @param tableName name of the table.
	 * @param tableInfo table description of the dataset.
	 * @param numberOfRecords the number of records.
	 */
	public VectorDatasetInfo(String identification, String title, String alternateTitle, String categoryId, Date revisionDate, Set<Attachment> attachments, 
		Set<Log> logs, String tableName, TableInfo tableInfo, long numberOfRecords) {		
		super(identification, title, alternateTitle, categoryId, revisionDate, attachments, logs);
		
		this.tableName = tableName;
		this.tableInfo = tableInfo;
		this.numberOfRecords = numberOfRecords;
	}
	
	/**
	 * 
	 * @return the name of the table
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * 
	 * @return table description
	 */
	public TableInfo getTableInfo() {
		return tableInfo;
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
		return "VectorDatasetInfo [tableName=" + tableName + ", tableInfo="
				+ tableInfo + ", numberOfRecords=" + numberOfRecords
				+ ", identification=" + identification + ", title=" + title
				+ ", alternateTitle=" + alternateTitle + ", categoryId="
				+ categoryId + ", revisionDate=" + revisionDate
				+ ", attachments=" + attachments + ", logs=" + logs + "]";
	}
	
}
