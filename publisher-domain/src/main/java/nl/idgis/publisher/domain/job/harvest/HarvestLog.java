package nl.idgis.publisher.domain.job.harvest;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.web.EntityType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HarvestLog extends MessageProperties {

	private static final long serialVersionUID = -4669631261794550765L;
	
	protected final String alternateTitle;
	
	public HarvestLog(final EntityType entityType, final String identification, final String title) {
		this(entityType, identification, title, null);
	}
	
	@JsonCreator
	public HarvestLog(
			@JsonProperty("entityType") EntityType entityType,
			@JsonProperty("identification") String identification,
			@JsonProperty("title") String title,
			@JsonProperty("alternateTitle") String alternateTitle) {
		
		super (entityType, identification, title);
		
		this.alternateTitle = alternateTitle;
	}
	
	public String getAlternateTitle() {
		return alternateTitle;
	}

	@Override
	public String toString() {
		return "HarvestLog [identification=" + getIdentification () + ", title="
				+ getTitle () + ", alternateTitle=" + getAlternateTitle () + "]";
	}
	
}
