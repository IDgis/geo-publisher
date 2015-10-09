package nl.idgis.publisher.metadata;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.metadata.messages.KeepMetadata;
import nl.idgis.publisher.metadata.messages.MetadataItemInfo;
import nl.idgis.publisher.metadata.messages.MetadataNotFound;
import nl.idgis.publisher.metadata.messages.UpdateMetadata;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

/**
 * This abstract metadata generator actor facilitates the interaction between metadata source,
 * document generators (i.e. subclasses of this class) and metadata target. It also provides
 * a few utility methods shared by the document generators.
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 * @param <T>
 */
public abstract class AbstractMetadataItemGenerator<T extends MetadataItemInfo> extends UntypedActor {
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef metadataTarget;
	
	protected final T itemInfo;
	
	private final String serviceLinkagePrefix, datasetMetadataPrefix;
	
	/**
	 * Different service types.
	 * 
	 * @author Reijer Copier <reijer.copier@idgis.nl>
	 *
	 */
	protected static enum ServiceType {
		
		WMS, WFS;
		
		String getProtocol() {
			return "OGC:" + name();
		}
	}
	
	private FutureUtils f;	
	
	/**
	 * Container to store {@link Failure} objects received from the metadata target.
	 * 
	 * @author Reijer Copier <reijer.copier@idgis.nl>
	 *
	 */
	private static class TargetResult {
		
		private final Set<Failure> failures;
		
		TargetResult(Set<Failure> failures) {
			this.failures = failures;
		}
		
		Set<Failure> getFailures() {
			return failures;
		}
	}
	
	AbstractMetadataItemGenerator(ActorRef metadataTarget, T itemInfo, String serviceLinkagePrefix, String datasetMetadataPrefix) {
		this.metadataTarget = metadataTarget;
		this.itemInfo = itemInfo;
		this.serviceLinkagePrefix = serviceLinkagePrefix;
		this.datasetMetadataPrefix = datasetMetadataPrefix;
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(5, TimeUnit.SECONDS));
		
		f = new FutureUtils(getContext());
	}
	
	/**
	 * Provides a list of {@link KeepMetadata} messages to be send to the message target. 
	 * 
	 * @return the list of messages.
	 */
	protected abstract List<? extends KeepMetadata> keepMetadata();
	
	/**
	 * Provides a list of {@link UpdateMetadata} messages to be send to the message target. 
	 *
	 * @param metadataDocument the received metadata document.
	 * @return the list of messages.
	 */
	protected abstract List<? extends UpdateMetadata> updateMetadata(MetadataDocument metadataDocument) throws Exception;

	/**
	 * Default behavior. Handles {@link MetadataDocument} and {@link MetadataNotFound}.
	 */
	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("message received while generating metadata document: {}", msg);
		
		if(msg instanceof MetadataDocument) {
			handleMetadataDocument((MetadataDocument)msg);
		} else if(msg instanceof MetadataNotFound) {
			handleMetadataNotFound();
		} else if(msg instanceof TargetResult) {
			handleTargetResult((TargetResult)msg);
		} else if(msg instanceof ReceiveTimeout) {
			handleReceiveTimeout();
		} else {
			unhandled(msg);
		}
	}

	/**
	 * Sends {@link KeepMetadata} messages to the message target.
	 */
	private void handleMetadataNotFound() {
		log.error("metadata not found");
		
		askMetadataTarget(keepMetadata());
	}

	/**
	 * Terminates the actor.
	 */
	private void handleReceiveTimeout() {
		log.error("timeout");
		
		stop();
	}

	/**
	 * Logs failures and terminates actor.
	 * @param the received {@link TargetResult} message.
	 */
	private void handleTargetResult(TargetResult msg) {
		log.debug("generator finished");
		
		Set<Failure> failures = msg.getFailures(); 
		if(failures.isEmpty()) {
			log.debug("no failures");
		} else {
			failures.forEach(failure ->
				log.warning("failure: {}", failure));
		}
		
		stop();
	}
	
	/**
	 * Send a list of request to the the message target and transforms the result
	 * into a single {@link TargetResult} object.
	 * 
	 * @param requests the list.
	 */
	private void askMetadataTarget(List<? extends Object> requests) {
		requests.stream()
			.map(request -> f.ask(metadataTarget, request)
				.exceptionally(t -> new Failure(t)))
			.collect(f.collect()).thenAccept(results -> {
				getSelf().tell(
					new TargetResult(
						results
							.filter(result -> result instanceof Failure)
							.map(result -> (Failure)result)
							.collect(Collectors.toSet())),
					getSelf());
			});
	}

	/**
	 * Obtains {@link UpdateMetadata} messages by processing the {@link MetadataDocument} object 
	 * and send the update requests to the message target.
	 * @param msg the received message.
	 */
	private void handleMetadataDocument(MetadataDocument msg) throws Exception {
		log.debug("metadata document received for item: {}", itemInfo.getId());
		
		msg.removeStylesheet();
		askMetadataTarget(updateMetadata(msg));
	}

	/**
	 * Terminates the actor.
	 */
	private void stop() {
		log.debug("terminating");
		getContext().stop(getSelf());
	}
	
	/**
	 * Computes a service linkage url.
	 * 
	 * @param serviceName the name of the service.
	 * @param serviceType the type of the service.
	 * @return the url.
	 */
	protected String getServiceLinkage(String serviceName, ServiceType serviceType) {
		return serviceLinkagePrefix + serviceName + "/" + serviceType.name().toLowerCase();
	}
	
	/**
	 * Computes a dataset metadata url.
	 * @param fileUuid
	 * @return the url.
	 */
	protected String getDatasetMetadataHref(String fileUuid) {
		return datasetMetadataPrefix + fileUuid + ".xml";
	}

}
