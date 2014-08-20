package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ActiveTask extends DashboardItem {

	private static final long serialVersionUID = 5377556601944221922L;
	
	private final String title;
	private final Integer progress; 

	@JsonCreator
	public ActiveTask(
			final @JsonProperty("id") String id,
			final @JsonProperty("title") String title,
			final @JsonProperty("message") Message message,
			final @JsonProperty("progress") Integer progress) {
		
		super(id, message);
		
		this.title = title;
		this.progress = progress;
	}
	
	@JsonGetter
	public String title () {
		return title;
	}
	
	@JsonGetter
	public Integer progress () {
		return this.progress;
	}

}
