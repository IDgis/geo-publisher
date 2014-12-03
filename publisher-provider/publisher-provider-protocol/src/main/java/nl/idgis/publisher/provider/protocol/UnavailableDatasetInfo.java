package nl.idgis.publisher.provider.protocol;

import java.util.Set;

public class UnavailableDatasetInfo extends DatasetInfo {
	
	private static final long serialVersionUID = 6422314524938004792L;

	public UnavailableDatasetInfo(String identification, String title, Set<Attachment> attachments, Set<Message<?>> messages) {
		super(identification, title, attachments, messages);
	}

	@Override
	public String toString() {
		return "UnavailableDatasetInfo [identification=" + identification + ", title=" + title
				+ ", attachments=" + attachments + "]";
	}

}
