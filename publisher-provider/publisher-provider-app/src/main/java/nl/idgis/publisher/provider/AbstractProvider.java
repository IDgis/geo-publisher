package nl.idgis.publisher.provider;

import java.util.Optional;

import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.protocol.AbstractGetDatasetRequest;
import nl.idgis.publisher.provider.protocol.EchoRequest;
import nl.idgis.publisher.provider.protocol.EchoResponse;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.provider.protocol.GetRasterDataset;
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

	private final DatasetInfoSourceDesc datasetInfoSourceDesc;
	
	protected ActorRef datasetInfoSource;
	
	public AbstractProvider(DatasetInfoSourceDesc datasetInfoSourceDesc) {
		this.datasetInfoSourceDesc = datasetInfoSourceDesc;
	}
	
	protected void preStartProvider() throws Exception {
		
	}
	
	@Override
	public final void preStart() throws Exception {
		datasetInfoSource = getContext().actorOf(datasetInfoSourceDesc.getProps(), "datasetInfoSource");
		
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
		} else if(msg instanceof GetRasterDataset) {
			handleGetRasterDataset((GetRasterDataset)msg);
		} else {
			unhandled(msg);
		}
	}
	
	protected Optional<Props> getVectorDatasetFetcher(GetVectorDataset msg) {
		return Optional.empty();
	}
	
	protected Optional<Props> getRasterDatasetFetcher(GetRasterDataset msg) {
		return Optional.empty();
	}
	
	private void handleGetVectorDataset(GetVectorDataset msg) {
		log.debug("get vector dataset");		
		performFetch(msg, getVectorDatasetFetcher(msg));
	}
	
	private void handleGetRasterDataset(GetRasterDataset msg) {
		log.debug("get raster dataset");		
		performFetch(msg, getRasterDatasetFetcher(msg));
	}

	private void performFetch(AbstractGetDatasetRequest msg, Optional<Props> optionalProps) {
		if(optionalProps.isPresent()) {
			log.debug("props obtained");
			
			ActorRef fetcher = getContext().actorOf(
				optionalProps.get(), 
				nameGenerator.getName(AbstractDatasetFetcher.class));
			
			datasetInfoSource.tell(datasetInfoSourceDesc.getRequest(msg.getIdentification()), fetcher);
		} else {
			unhandled(msg);
		}
	}
	
	protected abstract DatasetInfoBuilderPropsFactory getDatasetInfoBuilder();

	private void handleGetDatasetInfo(GetDatasetInfo msg) {
		log.debug("get dataset info");
		
		Props datasetInfoBuilderProps = getDatasetInfoBuilder().props(getSender(), msg.getAttachmentTypes());
		ActorRef datasetInfoBuilder = getContext().actorOf(
				datasetInfoBuilderProps,
				nameGenerator.getName(datasetInfoBuilderProps.actorClass()));
		
		datasetInfoSource.tell(datasetInfoSourceDesc.getRequest(msg.getIdentification()), datasetInfoBuilder);
	}
	
	private void handleListDatasetInfo(ListDatasetInfo msg) {
		log.debug("list dataset info");
		
		ActorRef converter = getContext().actorOf(
				DatasetInfoSourceStreamConverter.props(
					datasetInfoSourceDesc.getType(), 
					msg.getAttachmentTypes(), 
					datasetInfoSource, 
					getDatasetInfoBuilder()),
				nameGenerator.getName(DatasetInfoSourceStreamConverter.class));
		
		converter.forward(msg, getContext());
	}

}
