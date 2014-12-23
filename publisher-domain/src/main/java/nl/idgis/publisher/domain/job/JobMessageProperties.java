package nl.idgis.publisher.domain.job;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.MessageProperties;

public abstract class JobMessageProperties implements MessageProperties {

	private static final long serialVersionUID = -6116580247012880108L;

	private final EntityType entityType;
	
	private final String identification;
	
	private final String title;
	
	public JobMessageProperties(EntityType entityType, String identification, String title) {
	
		this.entityType = entityType;
		this.identification = identification;
		this.title = title;
	}

	@Override
	public EntityType getEntityType () {
		return entityType;
	}

	@Override
	public String getIdentification () {
		return identification;
	}

	@Override
	public String getTitle () {
		return title;
	}
}
