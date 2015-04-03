package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.EntityType;
import nl.idgis.publisher.domain.MessageProperties;
import nl.idgis.publisher.domain.StatusType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class DefaultMessageProperties implements MessageProperties {
	
	private static final long serialVersionUID = 240068555815262512L;

	private final EntityType entityType;
	
	private final String identification;
	
	private final String title;
	
	private final StatusType status;
	
	@JsonCreator
	public DefaultMessageProperties(
			final EntityType entityType,
			final String identification, 
			final String title,
			final StatusType status) {
	
		this.entityType = entityType;
		this.identification = identification;
		this.title = title;
		this.status = status;
	}

	@JsonValue
	public JsonNode serialize () {
		final ObjectNode obj = new ObjectMapper ().createObjectNode ();
		
		obj.put ("entityType", entityType.name ());
		obj.put ("identification", identification);
		obj.put ("title", title);
		if (status != null) {
			if (status instanceof Enum<?>) {
				obj.put ("status", status.getClass ().getCanonicalName ()  + "." + ((Enum<?>) status).name ());
			} else {
				obj.put ("status", status.toString ());
			}
		}
		
		return obj;
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

	@Override
	public StatusType getStatus () {
		return status;
	}
}
