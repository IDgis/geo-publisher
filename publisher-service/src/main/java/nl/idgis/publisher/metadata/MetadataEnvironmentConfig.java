package nl.idgis.publisher.metadata;

import java.nio.file.Path;

import com.typesafe.config.Config;

/**
 * This class contains metadata configuration information for a single environment.
 *  
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
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
	
	/** 
	 * @return the path to the service metadata documents.
	 */
	public Path getServiceMetadataTarget() {
		return serviceMetadataTarget;
	}
	
	/** 
	 * @return the path to the dataset metadata documents.
	 */
	public Path getDatasetMetadataTarget() {
		return datasetMetadataTarget;
	}
	
	/** 
	 * @return the service linkage url prefix.
	 */
	public String getServiceLinkagePrefix() {
		return config.getString("serviceLinkagePrefix");
	}
	
	/** 
	 * @return the name of the environment
	 */
	public String getName() {
		return name;
	}

	/** 
	 * @return the dataset metadata url prefix.
	 */
	public String getDatasetMetadataPrefix() {
		return config.getString("datasetMetadataPrefix");
	}
}
