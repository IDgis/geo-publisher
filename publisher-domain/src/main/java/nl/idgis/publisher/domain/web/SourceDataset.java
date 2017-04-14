package nl.idgis.publisher.domain.web;

import nl.idgis.publisher.domain.SourceDatasetType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SourceDataset extends Identifiable {
	
	private static final long serialVersionUID = -1456553405086131435L;
	
	private final String name, alternateTitle;
	private final EntityRef category;
	private final EntityRef dataSource;
	private final SourceDatasetType	type;
	private final boolean deleted;
	private final boolean confidential;
	private final boolean wmsOnly;
	private final String externalId;
	
	@JsonCreator
	public SourceDataset (
			final @JsonProperty("id") String id, 
			final @JsonProperty("name") String name,
			final @JsonProperty("alternateTitle") String alternateTitle, 
			final @JsonProperty("category") EntityRef category,
			final @JsonProperty("dataSource") EntityRef dataSource,
			final @JsonProperty("type") SourceDatasetType type,
			final @JsonProperty("deleted") boolean deleted,
			final @JsonProperty("confidential") boolean confidential,
			final @JsonProperty("wmsOnly") boolean wmsOnly,
			final @JsonProperty("externalId") String externalId) {
		super(id);
		
		if (type == null) {
			throw new NullPointerException ("type cannot be null");
		}
		
		this.name = name;
		this.alternateTitle = alternateTitle;
		this.category = category;
		this.dataSource = dataSource;
		this.type = type;
		this.deleted = deleted;
		this.confidential = confidential;
		this.wmsOnly = wmsOnly;
		this.externalId = externalId;
	}

	@JsonGetter
	public String name () {
		return this.name;
	}
	
	@JsonGetter
	public String alternateTitle () {
		return this.alternateTitle;
	}
	
	@JsonGetter
	public EntityRef category () {
		return this.category;
	}
	
	@JsonGetter
	public EntityRef dataSource () {
		return this.dataSource;
	}
	
	@JsonGetter
	public SourceDatasetType type () {
		return this.type;
	}
	
	@JsonGetter
	public boolean deleted () {
		return this.deleted;
	}
	
	@JsonGetter
	public boolean confidential () {
		return this.confidential;
	}
	
	@JsonGetter
	public boolean wmsOnly () {
		return this.wmsOnly;
	}
	
	@JsonGetter
	public String externalId() {
		return this.externalId;
	}
}
