package nl.idgis.publisher.provider.protocol;

import java.util.Set;

public class UnavailableDatasetInfo extends DatasetInfo {
	
	private static final long serialVersionUID = 6422314524938004792L;

	public UnavailableDatasetInfo(String id, String title, Set<Attachment> attachments, Set<Message<?>> messages) {
		super(id, title, attachments, messages);
	}

	@Override
	public String toString() {
		return "UnavailableDatasetInfo [id=" + id + ", title=" + title
				+ ", attachments=" + attachments + "]";
	}

}
