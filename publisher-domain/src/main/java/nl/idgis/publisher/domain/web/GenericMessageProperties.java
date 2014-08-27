package nl.idgis.publisher.domain.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.idgis.publisher.domain.MessageProperties;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericMessageProperties extends MessageProperties {

	private static final long serialVersionUID = 881732654261808765L;

	private final Map<String, Object> properties;
	
	public GenericMessageProperties (
			final EntityType entityType,
			final String identification, 
			final String title,
			final Map<String, Object> properties) {
		super(entityType, identification, title);
		
		this.properties = properties == null ? Collections.<String, Object>emptyMap () : new HashMap<> (properties); 
	}
	
	@JsonCreator
	public static GenericMessageProperties parse (final JsonNode node) {
		final ObjectMapper mapper = new ObjectMapper ();
		final Bean bean;
		
		try {
			bean = mapper.treeToValue (node, Bean.class);
		} catch (Exception e) {
			throw new IllegalArgumentException (e);
		}
		
		return new GenericMessageProperties (bean.getEntityType (), bean.getIdentification (), bean.getTitle (), bean.getProperties());
	}
	
	public EntityType entityType () {
		return getEntityType ();
	}
	
	public String identification () {
		return getIdentification ();
	}

	public String title () {
		return getTitle ();
	}
	
	public Map<String, Object> getProperties () {
		return Collections.unmodifiableMap (properties);
	}
	
	public final static class Bean {
		private EntityType entityType;
		private String identification;
		private String title;
		private Map<String, Object> properties = new HashMap<> ();
		
		public EntityType getEntityType() {
			return entityType;
		}
		
		public void setEntityType(EntityType entityType) {
			this.entityType = entityType;
		}
		
		public String getIdentification() {
			return identification;
		}
		
		public void setIdentification(String identification) {
			this.identification = identification;
		}
		
		public String getTitle() {
			return title;
		}
		
		public void setTitle(String title) {
			this.title = title;
		}
		
		@JsonAnyGetter
		public Map<String, Object> getProperties() {
			return properties;
		}
		
		@JsonAnySetter
		public void setProperty (final String name, final Object value) {
			properties.put (name, value);
		}
	}
}
