package nl.idgis.publisher.provider;

import java.util.Optional;

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

public abstract class AbstractProvider extends UntypedActor {
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	protected final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();

	private final Props metadataProps;
	
	protected ActorRef metadata;
	
	public AbstractProvider(Props metadataProps) {
		this.metadataProps = metadataProps;
	}
	
	protected void preStartProvider() throws Exception {
		
	}
	
	@Override
	public final void preStart() throws Exception {
		metadata = getContext().actorOf(metadataProps, "metadata");
		
		preStartProvider();
	}
	
	@Override
	public final void onReceive(Object msg) throws Exception {
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
	
	protected Optional<Props> getVectorDatasetFetcher(GetVectorDataset msg) {
		return Optional.empty();
	}
	
	private void handleGetVectorDataset(GetVectorDataset msg) {
		log.debug("get vector dataset");		
		
		
		Optional<Props> optionalProps = getVectorDatasetFetcher(msg);
		if(optionalProps.isPresent()) {
			log.debug("props obtained");
			
			ActorRef fetcher = getContext().actorOf(
				optionalProps.get(), 
				nameGenerator.getName(VectorDatasetFetcher.class));
			
			metadata.tell(new GetMetadata(msg.getIdentification()), fetcher);
		} else {
			unhandled(msg);
		}
	}
	
	protected abstract Props getDatasetInfoBuilder(GetDatasetInfo msg);

	private void handleGetDatasetInfo(GetDatasetInfo msg) {
		log.debug("get dataset info");
		
		ActorRef builder = getContext().actorOf(
				getDatasetInfoBuilder(msg),
				nameGenerator.getName(DatasetInfoBuilder.class));
		
		metadata.tell(new GetMetadata(msg.getIdentification()), builder);
	}
	
	protected abstract Props getDatasetInfoConverter(ListDatasetInfo msg);

	private void handleListDatasetInfo(ListDatasetInfo msg) {
		log.debug("list dataset info");
		
		ActorRef converter = getContext().actorOf(
				getDatasetInfoConverter(msg),
				nameGenerator.getName(DatasetInfoConverter.class));
		
		converter.forward(msg, getContext());
	}

}
