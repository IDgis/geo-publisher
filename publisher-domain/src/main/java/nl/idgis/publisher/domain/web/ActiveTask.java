package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ActiveTask extends DashboardItem {

	private static final long serialVersionUID = 5377556601944221922L;
	
	private final Integer progress; 

	@JsonCreator
	public ActiveTask(
			final @JsonProperty("id") String id,			 
			final @JsonProperty("message") Message message,
			final @JsonProperty("progress") Integer progress) {
		
		super(id, message);
		
		this.progress = progress;
	}
	
	@JsonGetter
	public int progress () {
		return this.progress;
	}

}
