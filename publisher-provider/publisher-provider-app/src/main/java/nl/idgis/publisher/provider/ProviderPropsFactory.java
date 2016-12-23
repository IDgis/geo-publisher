package nl.idgis.publisher.provider;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import com.typesafe.config.Config;

import nl.idgis.publisher.folder.Folder;
import nl.idgis.publisher.provider.metadata.MetadataDirectory;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.LoggingAdapter;

public class ProviderPropsFactory {
	
	private final LoggingAdapter log;
	
	private final Map<String, ActorRef> databases;
	
	public ProviderPropsFactory(LoggingAdapter log, Map<String, ActorRef> databases) {
		this.log = log;
		this.databases = databases;
	}
	
	private Props metadata(Config providerConfig) {
		return MetadataDirectory.props(new File(providerConfig.getString("metadata.folder")));		
	}
	
	private ActorRef getDatabase(String name) {
		if(databases.containsKey(name)) {
			log.info("using database: {}", name);
			return databases.get(name);
		} else {
			throw new IllegalArgumentException("unknown database: " + name);
		}
	}

	private ProviderProps vector(String name, Config providerConfig) {
		ActorRef database = getDatabase(providerConfig.getString("database"));
		Props metadata = metadata(providerConfig);
		
		log.info("creating vector provider: {}", name);
		
		return new ProviderProps(name, VectorProvider.props(database, new MetadataItemDatasetInfoSourceDesc(metadata)));
	}	
	
	private ProviderProps raster(String name, Config providerConfig) {
		Props folder = Folder.props(providerConfig.getString("data.folder"));
		Props metadata = metadata(providerConfig);
		
		log.info("creating raster provider: {}", name);
		
		return new ProviderProps(name, RasterProvider.props(folder, new MetadataItemDatasetInfoSourceDesc(metadata))); 
	}

	public Optional<ProviderProps> props(Config providerConfig) {
		String name = providerConfig.getString("name");
		String type = providerConfig.getString("type");
		
		switch(type) {
			case "VECTOR":
				return Optional.of(vector(name, providerConfig));
			case "RASTER":
				return Optional.of(raster(name, providerConfig));
		}
		
		log.error("unknown provider type: {}, name: {}", type, name);
		
		return Optional.empty();
	}
}
