package nl.idgis.publisher.metadata;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueType;

/**
 * Helper class to conveniently read configuration from a {@link Config} object.
 *   
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class MetadataConfig {

	private final Config config;
	
	private final FileSystem fileSystem;
	
	/**
	 * Creates MetadataConfig obtaining {@link Path} instances from the default file system.
	 * 
	 * @param config the config object.
	 */
	public MetadataConfig(Config config) {
		this(config, FileSystems.getDefault());
	}
	
	/**
	 * Creates a MetadataConfig.
	 * 
	 * @param config the config object.
	 * @param fileSystem the file system to obtain {@link Path} instances from.
	 */
	public MetadataConfig(Config config, FileSystem fileSystem) {
		this.config = config;
		this.fileSystem = fileSystem;
	}
	
	/** 
	 * @return configuration for the different environments.
	 */
	public Set<MetadataEnvironmentConfig> getEnvironments() {
		Path datasetTarget = fileSystem.getPath(config.getString("datasetTarget"));
		Path serviceTarget = fileSystem.getPath(config.getString("serviceTarget"));
		
		return config.getConfig("environments").root().entrySet().stream()			
			.flatMap(entry -> {
				String name = entry.getKey();
				ConfigValue value = entry.getValue();
				
				if(value.valueType() == ConfigValueType.OBJECT) {
					ConfigObject object = (ConfigObject)value;
					return Stream.of(new MetadataEnvironmentConfig(
						name, 
						object.toConfig(), 
						serviceTarget.resolve(name), 
						datasetTarget.resolve(name)));
				} else {
					return Stream.empty();
				}
			})
			.collect(Collectors.toSet());
	}
}
