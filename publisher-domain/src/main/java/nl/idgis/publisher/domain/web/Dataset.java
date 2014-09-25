package nl.idgis.publisher.domain.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Dataset extends Identifiable {

	private static final long serialVersionUID = -6846839007253396569L;
	
	private final String name;
	private final Category category;
	private final Status currentImportStatus, currentServiceStatus;
	private final List<DashboardItem> activeNotifications;
	private final EntityRef sourceDataset;
	private final Filter filterConditions;
	
	@JsonCreator
	public Dataset (
			final @JsonProperty("id") String id, 
			final @JsonProperty("name") String name,
			final @JsonProperty("category") Category category,
			final @JsonProperty("currentImportStatus") Status currentImportStatus,
			final @JsonProperty("currentServiceStatus") Status currentServiceStatus,
			final @JsonProperty("activeNotifications") List<DashboardItem> activeNotifications,
			final @JsonProperty("sourceDataset") EntityRef sourceDataset,
			final @JsonProperty("filterConditions") Filter filterConditions) {
		
		super (id);
		
		this.name = name;
		this.category = category;
		this.currentImportStatus = currentImportStatus;
		this.currentServiceStatus = currentServiceStatus;
		this.activeNotifications = activeNotifications == null ? Collections.<DashboardItem>emptyList () : new ArrayList<> (activeNotifications);
		this.sourceDataset = sourceDataset;
		this.filterConditions = filterConditions;
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
	public Status currentImportStatus () {
		return this.currentImportStatus;
	}
	
	@JsonGetter
	public Status currentServiceStatus () {
		return this.currentServiceStatus;
	}
	
	@JsonGetter
	public List<DashboardItem> activeNotifications () {
		return Collections.unmodifiableList (this.activeNotifications);
	}
	
	@JsonGetter
	public EntityRef sourceDataset () {
		return this.sourceDataset;
	}
	
	@JsonGetter
	public Filter filterConditions () {
		return this.filterConditions;
	}
}
