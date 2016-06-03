package util;

import java.util.Optional;

import javax.inject.Inject;

import play.Configuration;

public class MetadataConfig {
	
	private final String host, path, metadataUrlPrefix, zooKeeperHosts, zooKeeperNamespace, trustedAddresses, downloadUrlPrefix;
	
	@Inject
	public MetadataConfig(Configuration config) {

		Configuration metadata = config.getConfig("publisher.metadata");
		
		host = metadata.getString("host");
		path = "/";
		
		zooKeeperHosts = config.getString("zooKeeper.hosts");
		zooKeeperNamespace = config.getString("zooKeeper.namespace");
		
		trustedAddresses = metadata.getString("trusted-addresses", "");
		
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
	
	public String getTrustedAddresses() {
		return trustedAddresses;
	}
	
	public Optional<String> getDownloadUrlPrefix() {
		return Optional.ofNullable(downloadUrlPrefix);
	}
}
