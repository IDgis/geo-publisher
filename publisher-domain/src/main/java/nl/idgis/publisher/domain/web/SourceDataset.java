package nl.idgis.publisher.domain.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SourceDataset extends Identifiable {

	private static final long serialVersionUID = 3117616774767959933L;
	
	private final String name;
	private final Category category;
	private final EntityRef dataSource;
	
	@JsonCreator
	public SourceDataset (
			final @JsonProperty("id") String id, 
			final @JsonProperty("name") String name, 
			final @JsonProperty("category") Category category,
			final @JsonProperty("dataSource") EntityRef dataSource) {
		super(id);
		
		this.name = name;
		this.category = category;
		this.dataSource = dataSource;
	}

	@JsonGetter
	public String name () {
		return this.name;
	}
	
	@JsonGetter
	public Category category () {
		return this.category;
	}
	
	@JsonGetter
	public EntityRef dataSource () {
		return this.dataSource;
	}
}
