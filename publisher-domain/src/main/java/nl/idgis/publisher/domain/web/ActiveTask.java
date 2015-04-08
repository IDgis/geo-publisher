package nl.idgis.publisher.domain.web;

import java.sql.Timestamp;

import nl.idgis.publisher.domain.StatusType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ActiveTask extends DashboardItem {

	private static final long serialVersionUID = 5377556601944221922L;
	
	private final Boolean active;
	private final String title;
	private final Integer progress;
	private final Timestamp time; 

	@JsonCreator
	public ActiveTask(
			final @JsonProperty("id") String id,
			final @JsonProperty("title") String title,
			final @JsonProperty("message") Message message,
			final @JsonProperty("progress") Integer progress,
			final @JsonProperty("active") Boolean active) {
		
		super(id, message);
		
		this.title = title;
		this.progress = progress;
		this.active = active;
		this.time = new Timestamp(new java.util.Date().getTime());//currentTime
	}
	
	@JsonCreator
	public ActiveTask(
			final @JsonProperty("id") String id,
			final @JsonProperty("title") String title,
			final @JsonProperty("message") Message message,
			final @JsonProperty("time") Timestamp time,
			final @JsonProperty("active") Boolean active) {
		
		super(id, message);
		
		this.title = title;
		this.progress = null;
		this.active = active;
		this.time = time;
	}
	
	public Boolean active() {
		return active;
	}

	@JsonGetter
	public String title () {
		return title;
	}
	
	@JsonGetter
	public Integer progress () {
		return this.progress;
	}
	
	@JsonGetter
	public Timestamp time() {
		return time;
	}

	@JsonGetter
	public String getStatusString () {
		if (message() != null) {
			if (message().properties() != null) {
				StatusType status = message().properties().getStatus();
				if (status instanceof Enum<?>) {
					return status.getClass ().getCanonicalName ()  + "." + ((Enum<?>) status).name ();
				} else {
					return status.toString ();
				}
			}
		}
		return "";
	}
	
	@JsonGetter
	public String getStatusCategoryString () {
		if (message() != null) {
			if (message().properties() != null) {
				StatusType status = message().properties().getStatus();
				return status.statusCategory().getClass ().getCanonicalName ()  
					+ "." 
					+ status.statusCategory().name();
			}
		}
		return "";
	}


	
}
