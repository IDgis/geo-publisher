package nl.idgis.publisher.domain.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Dataset extends Identifiable {

	private static final long serialVersionUID = 4819080947382730225L;
	
	private final String name;
	private final Category category;
	private final Status currentStatus;
	private final List<DashboardItem> activeNotifications;
	private final EntityRef sourceDataset;
	
	@JsonCreator
	public Dataset (
			final @JsonProperty("id") String id, 
			final @JsonProperty("name") String name,
			final @JsonProperty("category") Category category,
			final @JsonProperty("currentStatus") Status currentStatus,
			final @JsonProperty("activeNotifications") List<DashboardItem> activeNotifications,
			final @JsonProperty("sourceDataset") EntityRef sourceDataset) {
		
		super (id);
		
		this.name = name;
		this.category = category;
		this.currentStatus = currentStatus;
		this.activeNotifications = activeNotifications == null ? Collections.<DashboardItem>emptyList () : new ArrayList<> (activeNotifications);
		this.sourceDataset = sourceDataset;
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
	public Status currentStatus () {
		return this.currentStatus;
	}
	
	@JsonGetter
	public List<DashboardItem> activeNotifications () {
		return Collections.unmodifiableList (this.activeNotifications);
	}
	
	@JsonGetter
	public EntityRef sourceDataset () {
		return this.sourceDataset;
	}
}
