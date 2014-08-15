package nl.idgis.publisher.domain.job.harvest;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HarvestLog implements Serializable {

	private static final long serialVersionUID = -4669631261794550765L;
	
	protected final String identification, title, alternateTitle;
	
	public HarvestLog(String identification) {
		this(identification, null, null);
	}
	
	@JsonCreator
	public HarvestLog(
			@JsonProperty("identification") String identification,
			@JsonProperty("title") String title,
			@JsonProperty("alternateTitle") String alternateTitle) {
		
		this.identification = identification;
		this.title = title;
		this.alternateTitle = alternateTitle;
	}
	
	public String getIdentification() {
		return identification;
	}

	public String getTitle() {
		return title;
	}

	public String getAlternateTitle() {
		return alternateTitle;
	}

	@Override
	public String toString() {
		return "HarvestLog [identification=" + identification + ", title="
				+ title + ", alternateTitle=" + alternateTitle + "]";
	}
	
}
