package nl.idgis.publisher.domain.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Dataset extends Identifiable {
	
	private static final long serialVersionUID = -7355311154563585294L;
	
	private final String name, metadataFileId;
	private final Category category;
	private final Status currentImportStatus;
	private final List<DashboardItem> activeNotifications;
	private final EntityRef sourceDataset;
	private final Filter filterConditions;
	private final long layerCount;
	private final long publishedServiceCount;
	private final boolean confidential;
	private final boolean wmsOnly;
	
	@JsonCreator
	public Dataset (
			final @JsonProperty("id") String id, 
			final @JsonProperty("name") String name,
			final @JsonProperty("category") Category category,
			final @JsonProperty("currentImportStatus") Status currentImportStatus,
			final @JsonProperty("activeNotifications") List<DashboardItem> activeNotifications,
			final @JsonProperty("sourceDataset") EntityRef sourceDataset,
			final @JsonProperty("filterConditions") Filter filterConditions,
			final @JsonProperty("layerCount") long layerCount,
			final @JsonProperty("publishedServiceCount") long publishedServiceCount,
			final @JsonProperty("confidential") boolean confidential,
			final @JsonProperty("wmsOnly") boolean wmsOnly,
			final @JsonProperty("metadataFileId") String metadataFileId) {
		
		super (id);
		
		this.name = name;
		this.category = category;
		this.currentImportStatus = currentImportStatus;
		this.activeNotifications = activeNotifications == null ? Collections.<DashboardItem>emptyList () : new ArrayList<> (activeNotifications);
		this.sourceDataset = sourceDataset;
		this.filterConditions = filterConditions;
		this.layerCount = layerCount;
		this.publishedServiceCount = publishedServiceCount;
		this.confidential = confidential;
		this.wmsOnly = wmsOnly;
		this.metadataFileId = metadataFileId;
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
	
	@JsonGetter
	public long layerCount () {
		return this.layerCount;
	}
	
	@JsonGetter
	public long publishedServiceCount () {
		return this.publishedServiceCount;
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
	public String metadataFileId () {
		return this.metadataFileId;
	}
}
