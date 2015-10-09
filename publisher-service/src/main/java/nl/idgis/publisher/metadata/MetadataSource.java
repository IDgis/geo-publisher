package nl.idgis.publisher.metadata;

import java.util.Objects;

import org.apache.commons.io.IOUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.sources.messages.GetMetadata;
import nl.idgis.publisher.metadata.messages.GetDatasetMetadata;
import nl.idgis.publisher.metadata.messages.GetServiceMetadata;
import nl.idgis.publisher.metadata.messages.MetadataNotFound;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

/**
 * This actor provides access to service and dataset metadata documents.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class MetadataSource extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final MetadataDocumentFactory documentFactory;
	
	private final ActorRef harvester;
	
	private FutureUtils f;
	
	public MetadataSource(ActorRef harvester) throws Exception {
		this.harvester = harvester;
		
		documentFactory = new MetadataDocumentFactory();
	}
	
	/**
	 * Creates a {@link Props} for the {@link MetadataSource} actor.
	 * 
	 * @param harvester a reference to the harvester.
	 * @return the props
	 */
	public static Props props(ActorRef harvester) {
		return Props.create(
			MetadataSource.class, 
			Objects.requireNonNull(harvester, "harvester must not be null")); 
	}
	
	@Override
	public final void preStart() throws Exception {
		f = new FutureUtils(getContext());
	}

	/**
	 * Default behavior. Handles {@link GetDatasetMetadata} and {@link GetServiceMetadata}.
	 * 
	 *@param msg the received message.
	 */
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

	/**
	 * Retrieves service metadata document.
	 * @param msg the received message.
	 */
	private void handleGetServiceMetadata(GetServiceMetadata msg) {
		log.debug("service metadata requested: {}", msg);
		
		try {
			byte[] documentContent = IOUtils.toByteArray(
				getClass().getResourceAsStream("service_metadata.xml"));
			
			getSender().tell(
				documentFactory.parseDocument(documentContent),
				getSelf());
		} catch(Exception e) {
			getSender().tell(new Failure(e), getSelf());
		}
	}

	/**
	 * Retrieves dataset metadata document.
	 * @param msg the received message.
	 */
	private void handleGetDatasetMetadata(GetDatasetMetadata msg) {
		log.debug("dataset metadata requested: {}", msg);
		
		ActorRef sender = getSender();
		f.ask(harvester, new GetDataSource(msg.getDataSourceId())).whenComplete((harvesterResponse, throwable) -> {
			if(throwable == null) {
				if(harvesterResponse instanceof ActorRef) {
					log.debug("actor reference received from harvester");					
					ActorRef dataSource = (ActorRef)harvesterResponse;
					dataSource.tell(new GetMetadata(msg.getExternalDatasetId()), sender);
				} else if(harvesterResponse instanceof NotConnected) {
					log.debug("data source is not connected");					
					sender.tell(new MetadataNotFound(), getSelf());
				} else {
					log.warning("unknown harvester response type: {}", harvesterResponse);					
					sender.tell(harvesterResponse, getSelf());
				}
			} else {
				log.error("error while retrieving dataset metadata");				
				sender.tell(new Failure(throwable), getSelf());
			}
		});
	}

}
