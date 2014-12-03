package nl.idgis.publisher.provider.protocol;

import java.util.Set;

import nl.idgis.publisher.domain.Log;

public class UnavailableDatasetInfo extends DatasetInfo {
	
	private static final long serialVersionUID = 5758296867365641240L;

	public UnavailableDatasetInfo(String identification, String title, Set<Attachment> attachments, Set<Log> logs) {
		super(identification, title, attachments, logs);
	}

	@Override
	public String toString() {
		return "UnavailableDatasetInfo [identification=" + identification + ", title=" + title
				+ ", attachments=" + attachments + "]";
	}

}
