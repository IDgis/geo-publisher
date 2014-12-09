package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;
import java.util.Set;

/**
 * Base class for all dataset info request messages.
 * 
 * @author copierrj
 *
 */
public abstract class AbstractDatasetInfoRequest implements Serializable {
	
	private static final long serialVersionUID = 1486078810924926358L;
	
	protected final Set<AttachmentType> attachmentTypes;
	
	AbstractDatasetInfoRequest(Set<AttachmentType> attachmentTypes) {
		this.attachmentTypes = attachmentTypes;
	}

	/**
	 * 
	 * @return which types of attachments should be included in the result
	 */
	public Set<AttachmentType> getAttachmentTypes() {
		return attachmentTypes;
	}
}
