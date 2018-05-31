package util;

import java.util.ArrayList;
import java.util.Optional;

import javax.inject.Inject;

import play.Configuration;

public class MetadataConfig {
	
	private final String host;
	
	private final String path;
	
	private final String metadataUrlPrefix;
	
	private final String zooKeeperHosts;
	
	private final String zooKeeperNamespace;
	
	private final String trustedHeader;
	
	private final Boolean viewerUrlDisplay;
	
	private final Boolean downloadUrlDisplay;
	
	private final Boolean rasterUrlDisplay;
	
	private final Boolean portalMetadataUrlDisplay;
	
	private final String downloadUrlPrefixExternal;
	
	private final String downloadUrlPrefixInternal;
	
	private final String rasterUrlPrefix;
	
	private final String portalMetadataUrlPrefixExternal;
	
	private final String portalMetadataUrlPrefixInternal;
	
	private final String viewerUrlPublicPrefix;
	
	private final String viewerUrlWmsOnlyPrefix;
	
	private final String viewerUrlSecurePrefix;
	
	private final String browseGraphicWmsRequest;
	
	private final String metadataStylesheetPrefix;
	
	private final boolean includeSourceDatasetMetadata;
	
	private final String acceptedDomainsUpdateStylesheet;
	
	@Inject
	public MetadataConfig(Configuration config) {

		Configuration metadata = config.getConfig("publisher.metadata");
		
		host = metadata.getString("host");
		path = "/";
		
		zooKeeperHosts = config.getString("zooKeeper.hosts");
		zooKeeperNamespace = config.getString("zooKeeper.namespace");
		
		trustedHeader = metadata.getString("trusted-header");
		
		viewerUrlDisplay = metadata.getBoolean("viewer-url-display");
		
		downloadUrlDisplay = metadata.getBoolean("download-url-display");
		
		rasterUrlDisplay = metadata.getBoolean("raster-url-display");
		
		portalMetadataUrlDisplay = metadata.getBoolean("portal-metadata-url-display");
		
		metadataUrlPrefix = "http://" + host + path + (path.endsWith("/") ? "" : "/") + "metadata/";
		
		downloadUrlPrefixExternal = metadata.getString("download-url-prefix-external");
		
		downloadUrlPrefixInternal = metadata.getString("download-url-prefix-internal");
		
		rasterUrlPrefix = metadata.getString("raster-url-prefix");
		
		portalMetadataUrlPrefixExternal = metadata.getString("portal-metadata-url-prefix-external");
		
		portalMetadataUrlPrefixInternal = metadata.getString("portal-metadata-url-prefix-internal");
		
		viewerUrlPublicPrefix = metadata.getString("viewer-url-prefix-public");
		
		viewerUrlWmsOnlyPrefix = metadata.getString("viewer-url-prefix-wmsonly");
		
		viewerUrlSecurePrefix = metadata.getString("viewer-url-prefix-secure");
		
		Integer fixedWidth = 600;
		String bbox = metadata.getString("bbox", "180000,459000,270000,540000");
		Integer height = getBrowseGraphicHeight(fixedWidth, bbox);
		
		browseGraphicWmsRequest = "?request=GetMap&service=WMS&SRS=EPSG:28992&CRS=EPSG:28992&bbox=" 
			+ bbox
			+ "&width=" + fixedWidth
			+ "&height=" + height
			+ "&format=image/png&styles=&layers=";
		
		metadataStylesheetPrefix = metadata.getString("stylesheet-url-prefix");
		
		includeSourceDatasetMetadata = metadata.getBoolean("include-source-dataset-metadata", true);
		
		acceptedDomainsUpdateStylesheet = metadata.getString("accepted-domains-update-stylesheet");
	}
	
	public String getHost() {
		return host;
	}
	
	public String getPath() {
		return path;
	}
	
	public Optional<String> getMetadataStylesheetPrefix() {
		return Optional.ofNullable(metadataStylesheetPrefix);
	}
	
	public String getMetadataUrlPrefix() {
		return metadataUrlPrefix;
	}
	
	public Optional<String> getZooKeeperHosts() {
		return Optional.ofNullable(zooKeeperHosts);
	}
	
	public Optional<String> getZooKeeperNamespace() {
		return Optional.ofNullable(zooKeeperNamespace);
	}
	
	public String getTrustedHeader() {
		return trustedHeader;
	}
	
	public Boolean getViewerUrlDisplay() {
		return viewerUrlDisplay;
	}
	
	public Boolean getDownloadUrlDisplay() {
		return downloadUrlDisplay;
	}
	
	public Boolean getRasterUrlDisplay() {
		return rasterUrlDisplay;
	}
	
	public Boolean getPortalMetadataUrlDisplay() {
		return portalMetadataUrlDisplay;
	}

	public Optional<String> getDownloadUrlPrefixExternal() {
		return Optional.ofNullable(downloadUrlPrefixExternal);
	}
	
	public Optional<String> getDownloadUrlPrefixInternal() {
		return Optional.ofNullable(downloadUrlPrefixInternal);
	}
	
	public Optional<String> getRasterUrlPrefix() {
		return Optional.ofNullable(rasterUrlPrefix);
	}
	
	public Optional<String> getPortalMetadataUrlPrefixExternal() {
		return Optional.ofNullable(portalMetadataUrlPrefixExternal);
	}
	
	public Optional<String> getPortalMetadataUrlPrefixInternal() {
		return Optional.ofNullable(portalMetadataUrlPrefixInternal);
	}
	
	public Optional<String> getViewerUrlWmsOnlyPrefix() {
		return Optional.ofNullable(viewerUrlWmsOnlyPrefix);
	}
	
	public Optional<String> getViewerUrlPublicPrefix() {
		return Optional.ofNullable(viewerUrlPublicPrefix);
	}
	
	public Optional<String> getViewerUrlSecurePrefix() {
		return Optional.ofNullable(viewerUrlSecurePrefix);
	}
	
	public Integer getBrowseGraphicHeight(Integer fixedWidth, String bbox) {
		ArrayList<Double> bboxItems = new ArrayList<>();
		
		for(String item : bbox.split(",")) {
			bboxItems.add(Double.parseDouble(item));
		}
		
		Double widthBbox = bboxItems.get(2) - bboxItems.get(0);
		Double heightBbox = bboxItems.get(3) - bboxItems.get(1);
		Double aspectRatio = widthBbox / heightBbox;
		Double height = fixedWidth / aspectRatio;
		
		return height.intValue();
	}
	
	public String getBrowseGraphicWmsRequest() {
		return browseGraphicWmsRequest;
	}
	
	public boolean getIncludeSourceDatasetMetadata() {
		return includeSourceDatasetMetadata;
	}
	
	public Optional<String> getAcceptedDomainsUpdateStylesheet() {
		return Optional.ofNullable(acceptedDomainsUpdateStylesheet);
	}
}
