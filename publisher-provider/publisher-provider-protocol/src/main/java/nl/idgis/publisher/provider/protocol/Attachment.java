package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;

public class Attachment implements Serializable {
	
	private static final long serialVersionUID = -6775797594339939002L;
	
	private final String id;
	
	private final AttachmentType attachmentType;
	
	private final Object content;
	
	public Attachment(String id, AttachmentType attachmentType, Object content) {		
		this.id = id;
		this.attachmentType = attachmentType;
		this.content = content;
	}
	
	public String getId() {
		return id;
	}

	public AttachmentType getAttachmentType() {
		return attachmentType;
	}

	public Object getContent() {
		return content;
	}

	@Override
	public String toString() {
		return "Attachment [id=" + id + ", attachmentType=" + attachmentType
				+ "]";
	}
}
