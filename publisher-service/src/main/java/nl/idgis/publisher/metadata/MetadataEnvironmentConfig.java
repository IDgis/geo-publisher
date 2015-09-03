package nl.idgis.publisher.metadata;

import java.nio.file.Path;

import com.typesafe.config.Config;

public class MetadataEnvironmentConfig {
	
	private final String name;

	private final Config config;
	
	private final Path serviceMetadataTarget, datasetMetadataTarget;	
	
	MetadataEnvironmentConfig(String name, Config config, Path serviceMetadataTarget, Path datasetMetadataTarget) {
		this.name = name;
		this.config = config;
		this.serviceMetadataTarget = serviceMetadataTarget;
		this.datasetMetadataTarget = datasetMetadataTarget;
	}
	
	public Path getServiceMetadataTarget() {
		return serviceMetadataTarget;
	}
	
	public Path getDatasetMetadataTarget() {
		return datasetMetadataTarget;
	}
	
	public String getServiceLinkagePrefix() {
		return config.getString("serviceLinkagePrefix");
	}
	
	public String getName() {
		return name;
	}

	public String getDatasetMetadataPrefix() {
		return config.getString("datasetMetadataPrefix");
	}
}
