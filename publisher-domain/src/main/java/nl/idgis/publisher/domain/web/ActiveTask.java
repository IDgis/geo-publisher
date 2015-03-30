package nl.idgis.publisher.domain.web;

import java.sql.Timestamp;

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
			final @JsonProperty("progress") Integer progress) {
		
		super(id, message);
		
		this.title = title;
		this.progress = progress;
		this.active = true;
		this.time = new Timestamp(new java.util.Date().getTime());//currentTime
	}
	
	@JsonCreator
	public ActiveTask(
			final @JsonProperty("id") String id,
			final @JsonProperty("title") String title,
			final @JsonProperty("message") Message message,
			final @JsonProperty("time") Timestamp time) {
		
		super(id, message);
		
		this.title = title;
		this.progress = null;
		this.active = false;
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

	public Timestamp time() {
		return time;
	}

}
