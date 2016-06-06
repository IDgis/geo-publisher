package util;

import java.util.Optional;

import javax.inject.Inject;

import play.Configuration;

public class MetadataConfig {
	
	private final String host, path, metadataUrlPrefix, zooKeeperHosts, zooKeeperNamespace, trustedHeader, downloadUrlPrefix;
	
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
}
