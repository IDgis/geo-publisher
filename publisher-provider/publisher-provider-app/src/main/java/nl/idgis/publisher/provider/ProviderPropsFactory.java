package nl.idgis.publisher.provider;

import java.io.File;
import java.util.Optional;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import nl.idgis.publisher.folder.Folder;
import nl.idgis.publisher.provider.database.Database;
import nl.idgis.publisher.provider.database.DatabaseType;
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
		Config databaseConfig = providerConfig.getConfig("database");
		Config rasterConfig = providerConfig.getConfig("raster");
		
		Props database = Database.props(databaseConfig, name);
		Props rasterFolder = Folder.props(providerConfig.getString("raster.folder"));
		
		log.info("creating sde provider: {}", name);
		
		return new ProviderProps(name, SDEProvider.props(database, rasterFolder, databaseConfig, rasterConfig));
	}

	private ProviderProps vector(String name, Config providerConfig) {
		Config databaseConfig = providerConfig.getConfig("database");
		Props database = Database.props(databaseConfig, name);
		Props metadata = metadata(providerConfig);
		
		log.info("creating vector provider: {}", name);
		
		DatabaseType databaseVendor = null;
		if (databaseConfig.hasPath("vendor")) {
			try {
				databaseVendor = DatabaseType.valueOf(databaseConfig.getString("vendor").toUpperCase());
			} catch(IllegalArgumentException iae) {
				throw new ConfigException.BadValue("vendor", "Invalid vendor supplied in config");
			}
		} else {
			databaseVendor = DatabaseType.ORACLE;
		}
		
		return new ProviderProps(name, VectorProvider.props(database, metadata, databaseVendor));
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
