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

import nl.idgis.publisher.metadata.messages.MetadataItemInfo;
import nl.idgis.publisher.metadata.messages.PutMetadata;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;

public abstract class AbstractMetadataItemGenerator<T extends MetadataItemInfo, U extends PutMetadata> extends UntypedActor {
	
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
	
	private static class GeneratorResult {
		
		private final Set<Failure> failures;
		
		GeneratorResult(Set<Failure> failures) {
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
	
	protected abstract List<? extends PutMetadata> generateMetadata(MetadataDocument metadataDocument) throws Exception;

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("message received while generating metadata document: {}", msg);
		
		if(msg instanceof MetadataDocument) {
			handleMetadataDocument((MetadataDocument)msg);
		} else if(msg instanceof GeneratorResult) {
			handleGeneratorResult((GeneratorResult)msg);
		} else if(msg instanceof ReceiveTimeout) {
			handleReceiveTimeout();
		} else {
			unhandled(msg);
		}
	}

	private void handleReceiveTimeout() {
		log.error("timeout");
		
		stop();
	}

	private void handleGeneratorResult(Object msg) {
		log.debug("generator finished");
		
		Set<Failure> failures = ((GeneratorResult) msg).getFailures(); 
		if(failures.isEmpty()) {
			log.debug("no failures");
		} else {
			failures.forEach(failure ->
				log.warning("failure: {}", failure));
		}
		
		stop();
	}

	private void handleMetadataDocument(MetadataDocument msg) throws Exception {
		log.debug("metadata document received for item: {}", itemInfo.getId());
		
		generateMetadata(msg).stream()
			.map(putMetadata -> f.ask(metadataTarget, putMetadata)
				.exceptionally(t -> new Failure(t)))
			.collect(f.collect()).thenAccept(results -> {
				getSelf().tell(
					new GeneratorResult(
						results
							.filter(result -> result instanceof Failure)
							.map(result -> (Failure)result)
							.collect(Collectors.toSet())),
					getSelf());
			});
	}

	private void stop() {
		log.debug("terminating");
		
		getContext().parent().tell(new NextItem(), getSelf());
		getContext().stop(getSelf());
	}
	
	protected String getServiceLinkage(String serviceId, ServiceType serviceType) {
		return serviceLinkagePrefix + serviceId + "/" + serviceType.name().toLowerCase();
	}
	
	protected String getDatasetMetadataHref(String fileUuid) {
		return datasetMetadataPrefix + fileUuid + ".xml";
	}

}
