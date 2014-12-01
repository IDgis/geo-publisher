package nl.idgis.publisher.provider;

import scala.concurrent.Future;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.pattern.Patterns;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.StreamConverter;
import nl.idgis.publisher.stream.messages.Item;

public class MetadataDatasetInfoConverter extends StreamConverter {
	
	private final ActorRef database;

	public MetadataDatasetInfoConverter(ActorRef target, ActorRef database) {
		super(target);
		
		this.database = database;
	}
	
	public static Props props(ActorRef target, ActorRef database) {
		return Props.create(MetadataDatasetInfoConverter.class, target, database);
	}

	@Override
	protected Future<Item> convert(Item item) {
		if(item instanceof MetadataItem) {
			return null;
		} else {
			return Futures.successful(item);
		} 
	}

}
