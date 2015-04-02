package nl.idgis.publisher.provider;

import java.io.File;
import java.util.Optional;

import com.typesafe.config.Config;

import nl.idgis.publisher.provider.database.Database;
import nl.idgis.publisher.provider.metadata.Metadata;

import akka.actor.Props;
import akka.event.LoggingAdapter;

public class ProviderPropsFactory {
	
	private final LoggingAdapter log;
	
	public ProviderPropsFactory(LoggingAdapter log) {
		this.log = log;
	}

	private ProviderProps vector(String name, Config providerConfig) {
		Props database = Database.props(providerConfig.getConfig("database"), name);
		Props metadata = Metadata.props(new File(providerConfig.getString("metadata.folder")));
		
		log.info("creating vector provider: {}", name);
		
		return new ProviderProps(name, Provider.props(database, metadata));
	}

	public Optional<ProviderProps> props(Config providerConfig) {
		String name = providerConfig.getString("name");
		String type = providerConfig.getString("type");
		
		switch(type) {
			case "VECTOR":
				return Optional.of(vector(name, providerConfig));
		}
		
		log.error("unknown provider type: {}, name: {}", type, name);
		
		return Optional.empty();
	}
}
