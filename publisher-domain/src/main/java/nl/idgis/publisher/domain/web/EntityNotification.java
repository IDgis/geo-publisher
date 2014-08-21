package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class EntityNotification extends Entity {

	private static final long serialVersionUID = 988121504973248478L;
	
	private final EntityRef entity;
	private final DashboardItem notification;
	
	@JsonCreator
	public EntityNotification (final @JsonProperty("entity") EntityRef entity, final @JsonProperty("notification") DashboardItem notification) {
		this.entity = entity;
		this.notification = notification;
	}
	
	@JsonGetter
	public EntityRef entity () {
		return entity;
	}
	
	@JsonGetter
	public DashboardItem notification () {
		return notification;
	}
}
