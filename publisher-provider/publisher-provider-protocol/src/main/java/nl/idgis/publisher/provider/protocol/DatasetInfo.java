package nl.idgis.publisher.provider.protocol;

import java.util.Set;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.stream.messages.Item;

public abstract class DatasetInfo extends Item {

	private static final long serialVersionUID = -1258083358767006453L;

	protected final String identification;
	
	protected final String title;
	
	protected final Set<Attachment> attachments;
	
	protected final Set<Log> logs;
			
	DatasetInfo(String identification, String title, Set<Attachment> attachments, Set<Log> logs) {
		this.identification = identification;
		this.title = title;		
		this.attachments = attachments;
		this.logs = logs;
	}
	
	public String getIdentification() {
		return identification;
	}
	
	public String getTitle() {
		return title;
	}	
	
	public Set<Attachment> getAttachments() {
		return attachments;
	}
	
	public Set<Log> getLogs() {
		return logs;
	}
	
}
