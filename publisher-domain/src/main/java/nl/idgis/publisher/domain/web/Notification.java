package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Notification extends DashboardItem {

	private static final long serialVersionUID = -8244495374549919962L;

	@JsonCreator
	public Notification(
			final @JsonProperty("id") String id,
			final @JsonProperty("message") Message message) {
		
		super(id, message);
	}
}
