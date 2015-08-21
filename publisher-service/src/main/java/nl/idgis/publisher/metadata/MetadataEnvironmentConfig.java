package nl.idgis.publisher.metadata;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import com.typesafe.config.Config;

public class MetadataEnvironmentConfig {

	private final Config config;
	
	private final FileSystem fileSystem;
	
	MetadataEnvironmentConfig(Config config, FileSystem fileSystem) {
		this.config = config;
		this.fileSystem = fileSystem;
	}
	
	public Path getServiceMetadataTarget() {
		return fileSystem.getPath(config.getString("serviceMetadataTarget"));
	}
	
	public Path getDatasetMetadataTarget() {
		return fileSystem.getPath(config.getString("datasetMetadataTarget"));
	}
	
	public String getName() {
		return config.getString("name");
	}
}
