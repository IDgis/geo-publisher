package nl.idgis.publisher.provider;

import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.metadata.messages.GetMetadata;
import nl.idgis.publisher.provider.protocol.EchoRequest;
import nl.idgis.publisher.provider.protocol.EchoResponse;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class VectorProvider extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final Props databaseProps, metadataProps;
	
	private ActorRef database, metadata;
	
	public VectorProvider(Props databaseProps, Props metadataProps) {
		this.databaseProps = databaseProps;
		this.metadataProps = metadataProps;
	}
	
	public static Props props(Props databaseProps, Props metadataProps) {
		return Props.create(VectorProvider.class, databaseProps, metadataProps);
	}
	
	@Override
	public void preStart() {
		database = getContext().actorOf(databaseProps, "database");
		metadata = getContext().actorOf(metadataProps, "metadata");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.info("registered with: " + ((Hello)msg).getName());
		} else if(msg instanceof EchoRequest) {
			getSender().tell(new EchoResponse(((EchoRequest) msg).getPayload()), getSelf());
		} else if(msg instanceof ListDatasetInfo) {
			handleListDatasetInfo((ListDatasetInfo)msg);
		} else if(msg instanceof GetDatasetInfo) {
			handleGetDatasetInfo((GetDatasetInfo)msg);
		} else if(msg instanceof GetVectorDataset) {
			handleGetVectorDataset((GetVectorDataset)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleGetVectorDataset(GetVectorDataset msg) {
		log.debug("get vector dataset");		
		
		ActorRef fetcher = getContext().actorOf(
				VectorDatasetFetcher.props(getSender(), database, msg), 
				nameGenerator.getName(VectorDatasetFetcher.class));
		
		metadata.tell(new GetMetadata(msg.getIdentification()), fetcher);
	}

	private void handleGetDatasetInfo(GetDatasetInfo msg) {
		log.debug("get dataset info");
		
		ActorRef builder = getContext().actorOf(
				DatasetInfoBuilder.props(getSender(), getSelf(), database, msg.getAttachmentTypes()),
				nameGenerator.getName(DatasetInfoBuilder.class));
		
		metadata.tell(new GetMetadata(msg.getIdentification()), builder);
	}

	private void handleListDatasetInfo(ListDatasetInfo msg) {
		log.debug("list dataset info");
		
		ActorRef converter = getContext().actorOf(
				DatasetInfoConverter.props(msg.getAttachmentTypes(), metadata, database),
				nameGenerator.getName(DatasetInfoConverter.class));
		
		converter.forward(msg, getContext());
	}

}
