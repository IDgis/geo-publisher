package nl.idgis.publisher.harvester.sources;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.harvester.sources.messages.FetchVectorDataset;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.provider.protocol.AttachmentType;
import nl.idgis.publisher.provider.protocol.EchoRequest;
import nl.idgis.publisher.provider.protocol.EchoResponse;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class ProviderDataSource extends UntypedActor {
	
	private final static FiniteDuration ECHO_INTERVAL = Duration.create(30, TimeUnit.SECONDS);
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final Set<AttachmentType> metadataType;
		
	private final ActorRef provider;
	
	public ProviderDataSource(ActorRef provider) {		
		this.provider = provider;
		
		metadataType = new HashSet<>();
		metadataType.add(AttachmentType.METADATA);
	}
	
	public static Props props(ActorRef provider) {
		return Props.create(ProviderDataSource.class, provider);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof EchoResponse) {
			handleEchoResponse((EchoResponse)msg);
		} else if(msg instanceof ListDatasets) {
			handleListDatasets((ListDatasets)msg);
		} else if(msg instanceof GetDatasetMetadata) {
			handleGetDatasetMetadata((GetDatasetMetadata)msg);
		} else if(msg instanceof FetchVectorDataset) {
			handleFetchVectorDataset((FetchVectorDataset)msg);
		} else {
			unhandled(msg);
		}
	}
	
	@Override
	public void preStart() throws Exception {
		scheduleEchoRequest(BigInteger.ZERO);
	}
	
	private void handleEchoResponse(EchoResponse msg) {
		log.debug("echo response: {}", msg);

		BigInteger payload = (BigInteger)msg.getPayload();
		scheduleEchoRequest(payload.add(BigInteger.ONE));
	}
	
	private void scheduleEchoRequest(BigInteger payload) {
		getContext().system().scheduler().scheduleOnce(
				ECHO_INTERVAL, provider, new EchoRequest(payload), 
					getContext().dispatcher(), getSelf());
	}
	
	private void handleListDatasets(ListDatasets msg) {
		log.debug("retrieving datasets from provider");
		
		ActorRef converter = getContext().actorOf(
				ProviderDatasetConverter.props(provider),
				nameGenerator.getName(ProviderDatasetConverter.class));
		
		converter.forward(msg, getContext());
	}
	
	private void handleFetchVectorDataset(final FetchVectorDataset msg) {
		log.debug("retrieving vector data from provider");
		
		Props receiverProps = msg.getReceiverProps();
		ActorRef receiver = getContext().actorOf(
				receiverProps, 
				nameGenerator.getName(receiverProps.clazz()));
		
		ActorRef initiator = getContext().actorOf(
				ProviderGetDatasetInitiater.props(getSender(), msg, receiver, provider),
				nameGenerator.getName(ProviderGetDatasetInitiater.class));
		
		provider.tell(new GetDatasetInfo(Collections.<AttachmentType>emptySet(), msg.getId()), initiator);
	}
	
	private void handleGetDatasetMetadata(GetDatasetMetadata msg) {				
		log.debug("retrieving dataset metadata from provider");
		
		ActorRef builder = getContext().actorOf(
				ProviderMetadataDocumentBuilder.props(getSender()),
				nameGenerator.getName(ProviderMetadataDocumentBuilder.class));
		
		provider.tell(new GetDatasetInfo(metadataType, msg.getDatasetId()), builder);
	}

}
