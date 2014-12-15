package nl.idgis.publisher.domain;

import java.io.Serializable;

import nl.idgis.publisher.domain.web.EntityType;

public abstract class MessageProperties implements Serializable {
	private static final long serialVersionUID = -6810242329459239754L;
	
	private final EntityType entityType;
	private final String identification;
	private final String title;
	
	public MessageProperties (final EntityType entityType, final String identification, final String title) {
		this.entityType = entityType;
		this.identification = identification;
		this.title = title;
	}

	public EntityType getEntityType () {
		return entityType;
	}

	public String getIdentification () {
		return identification;
	}

	public String getTitle () {
		return title;
	}
}
