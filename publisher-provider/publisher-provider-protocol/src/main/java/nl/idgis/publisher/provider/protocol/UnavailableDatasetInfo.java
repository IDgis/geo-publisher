package nl.idgis.publisher.provider.protocol;

import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

/**
 * Represents an unavailable dataset. 
 * It signifies that the dataset exists, but it cannot be retrieved at the moment.
 * 
 * @author copierrj
 *
 */
public class UnavailableDatasetInfo extends DatasetInfo {
	
	private static final long serialVersionUID = 1406423527757972234L;
	
	/**
	 * 
	 * @param identification the identifier of the dataset.
	 * @param title the title of the dataset.
	 * @param alternateTitle the alternate title of the dataset.
	 * @param categoryId the identifier of the category for this dataset
	 * @param revisionDate the revision date of this dataset
	 * @param attachments attachments of the dataset.
	 * @param logs logs for this dataset.
	 */
	public UnavailableDatasetInfo(String identification, String title, String alternateTitle, String categoryId, Date revisionDate, Set<Attachment> attachments, Set<Log> logs) {
		super(identification, title, alternateTitle, categoryId, revisionDate, attachments, logs);
	}

	@Override
	public String toString() {
		return "UnavailableDatasetInfo [identification=" + identification
				+ ", title=" + title + ", alternateTitle=" + alternateTitle
				+ ", categoryId=" + categoryId + ", revisionDate="
				+ revisionDate + ", attachments=" + attachments + ", logs="
				+ logs + "]";
	}	

}
