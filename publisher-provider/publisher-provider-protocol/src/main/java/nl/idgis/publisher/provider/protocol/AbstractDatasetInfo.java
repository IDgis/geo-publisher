package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;
import java.util.Set;

public abstract class AbstractDatasetInfo implements Serializable {
	
	private static final long serialVersionUID = 6308443905891150126L;
	
	protected final Set<AttachmentType> attachmentTypes;
	
	AbstractDatasetInfo(Set<AttachmentType> attachmentTypes) {
		this.attachmentTypes = attachmentTypes;
	}

	public Set<AttachmentType> getAttachmentTypes() {
		return attachmentTypes;
	}
}
