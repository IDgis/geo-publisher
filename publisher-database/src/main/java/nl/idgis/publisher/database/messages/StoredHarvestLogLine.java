package nl.idgis.publisher.database.messages;

import java.sql.Timestamp;

import com.mysema.query.annotations.QueryProjection;

import nl.idgis.publisher.domain.log.Events;
import nl.idgis.publisher.domain.log.HarvestLogLine;

public class StoredHarvestLogLine extends HarvestLogLine {
	
	private static final long serialVersionUID = -5803096205943876081L;

	@QueryProjection
	public StoredHarvestLogLine(String event, String dataSourceId, Timestamp createTime) {
		super(Events.toEvent(event), dataSourceId);
	}
}
