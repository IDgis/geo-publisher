package nl.idgis.publisher.provider.protocol;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import nl.idgis.publisher.domain.Log;

/**
 * Base class for all DatasetInfo response classes.
 * 
 * @author copierrj
 *
 */
public abstract class DatasetInfo implements Serializable {

	private static final long serialVersionUID = 4831203786160127733L;

	protected final String identification;
	
	protected final String title;
	
	protected final String alternateTitle;
	
	protected final String categoryId;
	
	protected final Date revisionDate;
	
	protected final Set<Attachment> attachments;
	
	protected final Set<Log> logs;
	
	protected final boolean confidential;
			
	DatasetInfo(String identification, String title, String alternateTitle, String categoryId, Date revisionDate, Set<Attachment> attachments, Set<Log> logs, boolean confidential) {
		this.identification = identification;
		this.title = title;		
		this.alternateTitle = alternateTitle;
		this.categoryId = categoryId;
		this.revisionDate = revisionDate;
		this.attachments = attachments;
		this.logs = logs;
		this.confidential = confidential;
	}
	
	/**
	 * 
	 * @return the dataset identification
	 */
	public String getIdentification() {
		return identification;
	}
	
	/**
	 * 
	 * @return the dataset title
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * 
	 * @return the dataset alternate title
	 */
	public String getAlternateTitle() {
		return alternateTitle;
	}
	
	/**
	 * 
	 * @return the category id
	 */
	public String getCategoryId() {
		return categoryId;
	}
	
	/**
	 * 
	 * @return the revision date
	 */
	public Date getRevisionDate() {
		return revisionDate;
	}
	
	/**
	 * 
	 * @return all requested attachments
	 */
	public Set<Attachment> getAttachments() {
		return attachments;
	}
	
	/**
	 * 
	 * @return all log messages
	 */
	public Set<Log> getLogs() {
		return logs;
	}
	
	/**
	 * 
	 * @return whether or not this dataset is confidential
	 */
	public boolean isConfidential() {
		return confidential;
	}
	
}
