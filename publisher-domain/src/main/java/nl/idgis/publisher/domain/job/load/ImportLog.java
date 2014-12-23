package nl.idgis.publisher.domain.job.load;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.job.JobMessageProperties;

public abstract class ImportLog extends JobMessageProperties {

	private static final long serialVersionUID = -462568737351384549L;

	public ImportLog(EntityType entityType, String identification, String title) {
		super(entityType, identification, title);
	}

}
