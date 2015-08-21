package nl.idgis.publisher.metadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.sources.messages.GetMetadata;
import nl.idgis.publisher.metadata.messages.GetDatasetMetadata;
import nl.idgis.publisher.metadata.messages.GetServiceMetadata;
import nl.idgis.publisher.metadata.messages.MetadataNotFound;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

public class MetadataSource extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final MetadataDocumentFactory documentFactory;
	
	private final ActorRef harvester;
	
	private final Path serviceMetadataDirectory;
	
	private FutureUtils f;
	
	public MetadataSource(ActorRef harvester, Path serviceMetadataDirectory) throws Exception {
		this.harvester = harvester;
		this.serviceMetadataDirectory = serviceMetadataDirectory;
		
		documentFactory = new MetadataDocumentFactory();
	}
	
	public static Props props(ActorRef harvester, Path serviceMetadataDirectory) {
		return Props.create(
			MetadataSource.class, 
			Objects.requireNonNull(harvester, "harvester must not be null"), 
			Objects.requireNonNull(serviceMetadataDirectory, "serviceMetadataDirectory must not be null"));
	}
	
	@Override
	public final void preStart() throws Exception {
		f = new FutureUtils(getContext());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetDatasetMetadata) {
			handleGetDatasetMetadata((GetDatasetMetadata)msg);
		} else if(msg instanceof GetServiceMetadata) {
			handleGetServiceMetadata((GetServiceMetadata)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleGetServiceMetadata(GetServiceMetadata msg) {
		log.debug("service metadata requested: {}", msg);
		
		try {
			Path serviceMetadataFile = serviceMetadataDirectory.resolve(msg.getServiceId() + ".xml");
			if(Files.isRegularFile(serviceMetadataFile)) {			
				byte[] documentContent = Files.readAllBytes(serviceMetadataFile);
				
				getSender().tell(
					documentFactory.parseDocument(documentContent),
					getSelf());
			} else {
				getSender().tell(new MetadataNotFound(), getSelf());
			}
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
	}

	private void handleGetDatasetMetadata(GetDatasetMetadata msg) {
		log.debug("dataset metadata requested: {}", msg);
		
		ActorRef sender = getSender();
		f.ask(harvester, new GetDataSource(msg.getDataSourceId())).whenComplete((harvesterResponse, throwable) -> {
			if(throwable == null) {
				if(harvesterResponse instanceof ActorRef) {
					ActorRef dataSource = (ActorRef)harvesterResponse;
					dataSource.tell(new GetMetadata(msg.getExternalDatasetId()), sender);
				} else {
					sender.tell(harvesterResponse, getSelf());
				}
			} else {
				sender.tell(new Failure(throwable), getSelf());
			}
		});
	}

}
