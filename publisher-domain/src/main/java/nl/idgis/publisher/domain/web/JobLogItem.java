package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class JobLogItem extends Entity {

	private final EntityRef job;
	private final LogItem logItem;
	
	@JsonCreator
	public JobLogItem (final @JsonProperty("job") EntityRef job, final @JsonProperty("logItem") LogItem logItem) {
		this.job = job;
		this.logItem = logItem;
	}
	
	@JsonGetter
	public EntityRef job () {
		return job;
	}
	
	@JsonGetter
	public LogItem logItem () {
		return logItem;
	}
}
