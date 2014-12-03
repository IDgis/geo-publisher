package nl.idgis.publisher.domain.job;

import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.web.EntityType;

public abstract class JobMessageProperties extends MessageProperties {
	
	private static final long serialVersionUID = -42941535009277205L;
	
	private final EntityType entityType;
	private final String identification;
	private final String title;
	
	public JobMessageProperties (final EntityType entityType, final String identification, final String title) {
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
