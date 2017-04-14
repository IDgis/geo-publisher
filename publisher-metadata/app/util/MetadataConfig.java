package util;

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
	
	private final Boolean downloadUrlDisplay;
	
	private final String downloadUrlPrefix;
	
	private final String viewerUrlPublicPrefix;
	
	private final String viewerUrlWmsOnlyPrefix;
	
	private final String viewerUrlSecurePrefix;
		
	private final String browseGraphicWmsRequest;
	
	private final String metadataStylesheetPrefix;
	
	private final boolean includeSourceDatasetMetadata;
	
	@Inject
	public MetadataConfig(Configuration config) {

		Configuration metadata = config.getConfig("publisher.metadata");
		
		host = metadata.getString("host");
		path = "/";
		
		zooKeeperHosts = config.getString("zooKeeper.hosts");
		zooKeeperNamespace = config.getString("zooKeeper.namespace");
		
		trustedHeader = metadata.getString("trusted-header");
		
		downloadUrlDisplay = metadata.getBoolean("download-url-display");
		
		metadataUrlPrefix = "http://" + host + path + (path.endsWith("/") ? "" : "/") + "metadata/";
		
		downloadUrlPrefix = metadata.getString("download-url-prefix");
		
		viewerUrlPublicPrefix = metadata.getString("viewer-url-prefix-public");
		
		viewerUrlWmsOnlyPrefix = metadata.getString("viewer-url-prefix-wmsonly");
		
		viewerUrlSecurePrefix = metadata.getString("viewer-url-prefix-secure");
		
		browseGraphicWmsRequest = "?request=GetMap&service=WMS&SRS=EPSG:28992&CRS=EPSG:28992&bbox=" 
			+ metadata.getString("bbox", "180000,459000,270000,540000")
			+ "&width=600&height=662&format=image/png&styles=&layers=";
		
		metadataStylesheetPrefix = metadata.getString("stylesheet-url-prefix");
		
		includeSourceDatasetMetadata = metadata.getBoolean("include-source-dataset-metadata", true);
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
	
	public Boolean getDownloadUrlDisplay() {
		return downloadUrlDisplay;
	}

	public Optional<String> getDownloadUrlPrefix() {
		return Optional.ofNullable(downloadUrlPrefix);
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
	
	public String getBrowseGraphicWmsRequest() {
		return browseGraphicWmsRequest;
	}
	
	public boolean getIncludeSourceDatasetMetadata() {
		return includeSourceDatasetMetadata;
	}
}
