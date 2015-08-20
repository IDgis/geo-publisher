package nl.idgis.publisher.provider.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import nl.idgis.publisher.provider.metadata.messages.BuildRefs;
import nl.idgis.publisher.provider.metadata.messages.GetAllMetadata;
import nl.idgis.publisher.provider.metadata.messages.GetDatasetId;
import nl.idgis.publisher.provider.metadata.messages.GetMetadataId;
import nl.idgis.publisher.provider.metadata.messages.IdNotFound;
import nl.idgis.publisher.provider.metadata.messages.MetadataItem;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorWithStash;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class DatasetMetadataRefs extends UntypedActorWithStash {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef metadataStore;
	
	private Map<String, String> datasetMetadataIds, metadataDatasetIds;
	
	public DatasetMetadataRefs(ActorRef metadataStore) {
		this.metadataStore = metadataStore;
	}
	
	public static Props props(ActorRef metadataStore) {
		return Props.create(
			DatasetMetadataRefs.class, 
			Objects.requireNonNull(metadataStore, "metadataStore must not be null"));
	}
	
	private void sendId(Map<String, String> idMap, String id) {
		if(idMap == null) {
			log.debug("no map -> stashing + start building");			
			getSelf().tell(new BuildRefs(), getSelf());
			stash();
		}
		
		getSender().tell(
			idMap.containsKey(id)
				? idMap.get(id)
				:  new IdNotFound(),
			getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof BuildRefs) {
			getContext().become(building());
			metadataStore.tell(new GetAllMetadata(), getSelf());
		} else if(msg instanceof GetMetadataId) {
			sendId(datasetMetadataIds, ((GetMetadataId) msg).getDatasetId());
		} else if(msg instanceof GetDatasetId) {			
			sendId(metadataDatasetIds, ((GetDatasetId) msg).getMetadataId());
		} else {
			unhandled(msg);
		}
	}
	
	protected interface IdPair {
		
		String getDatasetId();
		
		String getMetadataId();
	}
	
	protected Optional<IdPair> getIdPair(MetadataItem metadataItem) {
		return Optional.empty();
	}
	
	private Procedure<Object> building() {
		log.debug("(re)building maps");
		
		datasetMetadataIds = new HashMap<>();
		metadataDatasetIds = new HashMap<>();
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Item) {
					Item<?> item = (Item<?>)msg;
					
					Object itemContent = item.getContent();
					if(itemContent instanceof MetadataItem) {
						log.debug("metadata item received");
						
						getIdPair((MetadataItem)itemContent).ifPresent(idPair -> {
							String datasetId = idPair.getDatasetId();
							String metadataId = idPair.getMetadataId();
							
							datasetMetadataIds.put(datasetId, metadataId);
							metadataDatasetIds.put(metadataId, datasetId);
						});
					} else {
						log.error("unknown item received: {}", itemContent);
					}
					
					getSender().tell(new NextItem(), getSelf());
				} else if(msg instanceof End) {
					log.debug("all items received, result count: {}", datasetMetadataIds.size());
					
					getContext().become(receive());
					unstashAll();
				} else {
					log.debug("building -> stashing: {}", msg);
					stash();
				}
				
			}
			
		};
	}
}
