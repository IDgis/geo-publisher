package nl.idgis.publisher.harvester.sources;

import nl.idgis.publisher.harvester.sources.messages.Dataset;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ProviderDataset extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef harvester, database;
	
	public ProviderDataset(ActorRef harvester, ActorRef database) {
		this.harvester = harvester;
		this.database = database;
	}
	
	public static Props props(ActorRef harvester, ActorRef database) {
		return Props.create(ProviderDataset.class, harvester, database);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof MetadataItem) {
			log.debug("metadata item received");
			
			MetadataItem metadataItem = (MetadataItem)msg;
			
			final ActorRef sender = getSender(), self = getSelf();
			harvester.tell(new Dataset(metadataItem.getIdentification(), metadataItem.getTitle()), getContext().parent());
			sender.tell(new NextItem(), self);
		} else if(msg instanceof End) {	
			log.debug("dataset retrieval completed");
			
			getContext().stop(getSelf());
		} else if(msg instanceof Failure) {
			log.error(msg.toString());
			
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
}
