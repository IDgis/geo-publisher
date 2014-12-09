package nl.idgis.publisher.provider.protocol;

import java.util.Set;

import nl.idgis.publisher.stream.messages.Start;

/**
 * Request all {@link DatasetInfo} objects.
 * 
 * @author copierrj
 *
 */
public class ListDatasetInfo extends AbstractDatasetInfoRequest implements Start {
	
	private static final long serialVersionUID = 5440355202565901065L;

	/**
	 * 
	 * @param attachmentTypes specifies which attachment types to fetch.
	 */
	public ListDatasetInfo(Set<AttachmentType> attachmentTypes) {
		super(attachmentTypes);
	}

	@Override
	public String toString() {
		return "ListDatasetInfo [attachmentTypes=" + attachmentTypes + "]";
	}
	
}
