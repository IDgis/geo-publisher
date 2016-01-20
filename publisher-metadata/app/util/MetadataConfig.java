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
	
	private final String host, path, urlPrefix;
	
	private final Map<String, String> environmentPrefixes;

	@Inject
	public MetadataConfig(Configuration config) {
		Map<String, String> environmentPrefixes = new HashMap<>();
		Configuration environments = config.getConfig("publisher.metadata.environments");
		environments.entrySet().forEach(entry -> {
			String environmentId = entry.getKey();

			ConfigValue configValue = entry.getValue();
			if(configValue.valueType().equals(ConfigValueType.STRING)) {
				String environmentPrefix = configValue.unwrapped().toString();
				environmentPrefixes.put(environmentId, environmentPrefix);
			}
		});
		
		this.environmentPrefixes = Collections.unmodifiableMap(environmentPrefixes);
		
		host = config.getString("publisher.metadata.host");
		path = "/metadata";
		urlPrefix = "http://" + host + path + "/";
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
	
	public String getUrlPrefix() {
		return urlPrefix;
	}
}
