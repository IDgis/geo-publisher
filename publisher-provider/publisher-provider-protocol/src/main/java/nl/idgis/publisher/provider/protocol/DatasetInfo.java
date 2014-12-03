package nl.idgis.publisher.provider.protocol;

import java.util.Set;

import nl.idgis.publisher.stream.messages.Item;

public abstract class DatasetInfo extends Item {

	private static final long serialVersionUID = -7460162022665523517L;

	protected final String identification;
	
	protected final String title;
	
	protected final Set<Attachment> attachments;
	
	protected final Set<Message<?>> messages;
			
	DatasetInfo(String identification, String title, Set<Attachment> attachments, Set<Message<?>> messages) {
		this.identification = identification;
		this.title = title;		
		this.attachments = attachments;
		this.messages = messages;
	}
	
	public String getId() {
		return identification;
	}
	
	public String getTitle() {
		return title;
	}	
	
	public Set<Attachment> getAttachments() {
		return attachments;
	}
	
	public Set<Message<?>> getMessages() {
		return messages;
	}
	
}
