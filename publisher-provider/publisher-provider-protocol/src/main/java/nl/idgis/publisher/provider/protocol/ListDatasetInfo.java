package nl.idgis.publisher.provider.protocol;

import java.util.Set;

import nl.idgis.publisher.stream.messages.Start;

public class ListDatasetInfo extends AbstractDatasetInfo implements Start {
	
	private static final long serialVersionUID = 5440355202565901065L;

	public ListDatasetInfo(Set<AttachmentType> attachmentTypes) {
		super(attachmentTypes);
	}

	@Override
	public String toString() {
		return "ListDatasetInfo [attachmentTypes=" + attachmentTypes + "]";
	}
	
}
