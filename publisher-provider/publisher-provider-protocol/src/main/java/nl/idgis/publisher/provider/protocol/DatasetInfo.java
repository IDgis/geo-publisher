package nl.idgis.publisher.provider.protocol;

import java.util.Set;

import nl.idgis.publisher.stream.messages.Item;

public abstract class DatasetInfo extends Item {

	private static final long serialVersionUID = -7460162022665523517L;

	protected final String id;
	
	protected final String title;
	
	protected final Set<Attachment> attachments;
			
	DatasetInfo(String id, String title, Set<Attachment> attachments) {
		this.id = id;
		this.title = title;		
		this.attachments = attachments;
	}
	
	public String getId() {
		return id;
	}
	
	public String getTitle() {
		return title;
	}	
	
	public Set<Attachment> getAttachments() {
		return attachments;
	}
	
}
