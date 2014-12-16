package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

/**
 * A {@link DatasetInfo} attachment. It is untyped, but it does include an {@link AttachmentType} enum value.  
 * 
 * @author copierrj
 *
 */
public class Attachment implements Serializable {

	private static final long serialVersionUID = 3109003714510086835L;

	private final String identification;
	
	private final AttachmentType attachmentType;
	
	private final Object content;
	
	/**
	 * Creates an attachments.
	 * @param identification the identifier for this attachment.
	 * @param attachmentType the attachment type.
	 * @param content the actual attachment value.
	 */
	public Attachment(String identification, AttachmentType attachmentType, Object content) {		
		this.identification = identification;
		this.attachmentType = attachmentType;
		this.content = content;
	}
	
	/**
	 * 
	 * @return the identifier of this attachment
	 */
	public String getIdentification() {
		return identification;
	}

	/**
	 * 
	 * @return the attachment type
	 */
	public AttachmentType getAttachmentType() {
		return attachmentType;
	}

	/**
	 * 
	 * @return the actual value of the attachment
	 */
	public Object getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "Attachment [identification=" + identification + ", attachmentType=" + attachmentType
				+ "]";
	}
}
