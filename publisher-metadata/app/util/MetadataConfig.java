package util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

import play.Configuration;

public class MetadataConfig {
	
	private final String host, path, metadataUrlPrefix, zooKeeperHosts, zooKeeperNamespace, trustedAddresses;
	
	private final Map<String, String> environmentPrefixes;

	@Inject
	public MetadataConfig(Configuration config) {
		Map<String, String> environmentPrefixes = new HashMap<>();
		Configuration metadata = config.getConfig("publisher.metadata");
		
		Configuration environments = metadata.getConfig("environments");
		environments.entrySet().forEach(entry -> {
			String environmentId = entry.getKey();

			ConfigValue configValue = entry.getValue();
			if(configValue.valueType().equals(ConfigValueType.STRING)) {
				String environmentPrefix = configValue.unwrapped().toString();
				environmentPrefixes.put(environmentId, environmentPrefix);
			}
		});
		
		this.environmentPrefixes = Collections.unmodifiableMap(environmentPrefixes);
		
		host = metadata.getString("host");
		path = "/";
		
		zooKeeperHosts = config.getString("zooKeeper.hosts");
		zooKeeperNamespace = config.getString("zooKeeper.namespace");
		
		trustedAddresses = metadata.getString("trusted-addresses", "");
		
		metadataUrlPrefix = "http://" + host + path + (path.endsWith("/") ? "" : "/") + "metadata/";
	}
	
	public String getHost() {
		return host;
	}
	
	public String getPath() {
		return path;
	}
	
	public Optional<String> getEnvironmentUrlPrefix(String environmentId) {
		return Optional.ofNullable(environmentPrefixes.get(environmentId));
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
}
