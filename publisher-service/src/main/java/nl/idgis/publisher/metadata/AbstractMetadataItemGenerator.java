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

public abstract class AbstractMetadataItemGenerator<T extends MetadataItemInfo> extends UntypedActor {
	
	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef metadataTarget;
	
	protected final T itemInfo;
	
	private final String serviceLinkagePrefix, datasetMetadataPrefix;
	
	protected static enum ServiceType {
		
		WMS, WFS;
		
		String getProtocol() {
			return "OGC:" + name();
		}
	}
	
	private FutureUtils f;	
	
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
	
	protected abstract List<? extends KeepMetadata> keepMetadata();
	
	protected abstract List<? extends UpdateMetadata> updateMetadata(MetadataDocument metadataDocument) throws Exception;

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

	private void handleMetadataNotFound() {
		log.error("metadata not found");
		
		askMetadataTarget(keepMetadata());
	}

	private void handleReceiveTimeout() {
		log.error("timeout");
		
		stop();
	}

	private void handleTargetResult(Object msg) {
		log.debug("generator finished");
		
		Set<Failure> failures = ((TargetResult) msg).getFailures(); 
		if(failures.isEmpty()) {
			log.debug("no failures");
		} else {
			failures.forEach(failure ->
				log.warning("failure: {}", failure));
		}
		
		stop();
	}
	
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

	private void handleMetadataDocument(MetadataDocument msg) throws Exception {
		log.debug("metadata document received for item: {}", itemInfo.getId());
		
		msg.removeStylesheet();
		askMetadataTarget(updateMetadata(msg));
	}

	private void stop() {
		log.debug("terminating");
		getContext().stop(getSelf());
	}
	
	protected String getServiceLinkage(String serviceName, ServiceType serviceType) {
		return serviceLinkagePrefix + serviceName + "/" + serviceType.name().toLowerCase();
	}
	
	protected String getDatasetMetadataHref(String fileUuid) {
		return datasetMetadataPrefix + fileUuid + ".xml";
	}

}
