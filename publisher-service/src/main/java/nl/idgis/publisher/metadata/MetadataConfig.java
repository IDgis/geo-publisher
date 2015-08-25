package nl.idgis.publisher.metadata;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import com.typesafe.config.Config;

public class MetadataConfig {

	private final Config config;
	
	private final FileSystem fileSystem;
	
	public MetadataConfig(Config config) {
		this(config, FileSystems.getDefault());
	}
	
	public MetadataConfig(Config config, FileSystem fileSystem) {
		this.config = config;
		this.fileSystem = fileSystem;
	}
	
	public Path getServiceMetadataSource() {
		return fileSystem.getPath(config.getString("serviceMetadataSource"));
	}
	
	public Set<MetadataEnvironmentConfig> getEnvironments() {
		return config.getConfigList("environments").stream()
			.map(environmentConfig -> new MetadataEnvironmentConfig(environmentConfig, fileSystem))
			.collect(Collectors.toSet());
	}
}
