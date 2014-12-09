package nl.idgis.publisher.provider.protocol;

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
	
	private static final long serialVersionUID = 5758296867365641240L;

	/**
	 * 
	 * @param identification the identifier of the dataset.
	 * @param title the title of the dataset.
	 * @param attachments attachments of the dataset.
	 * @param logs logs for this dataset.
	 */
	public UnavailableDatasetInfo(String identification, String title, Set<Attachment> attachments, Set<Log> logs) {
		super(identification, title, attachments, logs);
	}

	@Override
	public String toString() {
		return "UnavailableDatasetInfo [identification=" + identification + ", title=" + title
				+ ", attachments=" + attachments + "]";
	}

}
