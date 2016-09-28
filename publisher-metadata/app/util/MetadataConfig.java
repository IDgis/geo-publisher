package util;

import java.util.Optional;

import javax.inject.Inject;

import play.Configuration;

public class MetadataConfig {
	
	private final String host, path, metadataUrlPrefix, zooKeeperHosts, zooKeeperNamespace, trustedHeader, downloadUrlPrefix, browseGraphicWmsRequest;
	
	@Inject
	public MetadataConfig(Configuration config) {

		Configuration metadata = config.getConfig("publisher.metadata");
		
		host = metadata.getString("host");
		path = "/";
		
		zooKeeperHosts = config.getString("zooKeeper.hosts");
		zooKeeperNamespace = config.getString("zooKeeper.namespace");
		
		trustedHeader = metadata.getString("trusted-header");
		
		metadataUrlPrefix = "http://" + host + path + (path.endsWith("/") ? "" : "/") + "metadata/";
		
		downloadUrlPrefix = metadata.getString("download-url-prefix");
		
		browseGraphicWmsRequest = "?request=GetMap&service=WMS&SRS=EPSG:28992&CRS=EPSG:28992&bbox=" 
			+ metadata.getString("bbox", "180000,459000,270000,540000")
			+ "&width=600&height=662&format=image/png&styles=&layers=";
	}
	
	public String getHost() {
		return host;
	}
	
	public String getPath() {
		return path;
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
	
	public Optional<String> getDownloadUrlPrefix() {
		return Optional.ofNullable(downloadUrlPrefix);
	}
	
	public String getBrowseGraphicWmsRequest() {
		return browseGraphicWmsRequest;
	}
}
