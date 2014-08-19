package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DashboardItem extends Identifiable {

	private static final long serialVersionUID = -6668318918440675297L;
	
	private final Message message;
	
	@JsonCreator
	public DashboardItem(
			final @JsonProperty("id") String id,
			final @JsonProperty("message") Message message)	{
		super(id);
		
		this.message = message;		
	}	
	
	@JsonGetter
	public Message message () {
		return this.message;
	}	

}
