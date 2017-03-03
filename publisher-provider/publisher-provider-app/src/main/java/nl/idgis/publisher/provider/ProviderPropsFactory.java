package nl.idgis.publisher.provider;

import java.io.File;
import java.util.Optional;

import com.typesafe.config.Config;

import nl.idgis.publisher.folder.Folder;
import nl.idgis.publisher.provider.database.Database;
import nl.idgis.publisher.provider.metadata.Metadata;

import akka.actor.Props;
import akka.event.LoggingAdapter;

public class ProviderPropsFactory {
	
	private final LoggingAdapter log;
	
	public ProviderPropsFactory(LoggingAdapter log) {
		this.log = log;
	}
	
	private Props metadata(Config providerConfig) {
		return Metadata.props(new File(providerConfig.getString("metadata.folder")));		
	}
	
	private ProviderProps sde(String name, Config providerConfig) {
		Props database = Database.props(providerConfig.getConfig("database"), name);
		Props rasterFolder = Folder.props(providerConfig.getString("raster.folder"));
		
		boolean includeConfidential;
		if(providerConfig.hasPath("include-confidential")) {
			includeConfidential = providerConfig.getBoolean("include-confidential");
		} else {
			includeConfidential = true;
		}
		
		log.info("creating sde provider: {}", name);
		
		return new ProviderProps(name, SDEProvider.props(database, rasterFolder, includeConfidential));
	}

	private ProviderProps vector(String name, Config providerConfig) {
		Props database = Database.props(providerConfig.getConfig("database"), name);
		Props metadata = metadata(providerConfig);
		
		log.info("creating vector provider: {}", name);
		
		return new ProviderProps(name, VectorProvider.props(database, metadata));
	}
	
	private ProviderProps raster(String name, Config providerConfig) {
		Props folder = Folder.props(providerConfig.getString("data.folder"));
		Props metadata = metadata(providerConfig);
		
		log.info("creating raster provider: {}", name);
		
		return new ProviderProps(name, RasterProvider.props(folder, metadata)); 
	}

	public Optional<ProviderProps> props(Config providerConfig) {
		String name = providerConfig.getString("name");
		String type = providerConfig.getString("type");
		
		switch(type) {
			case "SDE":
				return Optional.of(sde(name, providerConfig));
			case "VECTOR":
				return Optional.of(vector(name, providerConfig));
			case "RASTER":
				return Optional.of(raster(name, providerConfig));
		}
		
		log.error("unknown provider type: {}, name: {}", type, name);
		
		return Optional.empty();
	}
}
