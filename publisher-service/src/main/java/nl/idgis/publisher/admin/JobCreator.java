package nl.idgis.publisher.admin;

import java.util.concurrent.CompletableFuture;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.domain.query.RefreshDataset;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.Service;

import nl.idgis.publisher.job.manager.messages.CreateImportJob;
import nl.idgis.publisher.job.manager.messages.CreateServiceJob;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.manager.messages.GetServicesWithLayer;
import nl.idgis.publisher.utils.TypedIterable;

public class JobCreator extends AbstractAdmin {
	
	private final ActorRef serviceManager, jobSystem;

	public JobCreator(ActorRef database, ActorRef serviceManager, ActorRef jobSystem) {
		super(database);
		
		this.serviceManager = serviceManager;
		this.jobSystem = jobSystem;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager, ActorRef jobSystem) {
		return Props.create(JobCreator.class, database, serviceManager, jobSystem);
	}
	
	private void createServiceJob(String serviceId) {
		log.debug("creating service job: {}", serviceId);
		
		jobSystem.tell(new CreateServiceJob(serviceId), getSelf());
	}
	
	private void createServiceJobs(TypedIterable<?> serviceIds) {
		for(String serviceId : serviceIds.cast(String.class)) {
			createServiceJob(serviceId);
		}
	}
	
	private void createServiceJobsForLayer(String layerId) {
		log.debug("creating service jobs for layer: {}", layerId);
		
		f.ask(serviceManager, new GetServicesWithLayer(layerId), TypedIterable.class).thenAccept(resp -> {			
			TypedIterable<String> serviceIds = ((TypedIterable<?>)resp).cast(String.class);
			
			for(String serviceId : serviceIds) {
				log.debug("creating service job: {}", serviceId);
				
				jobSystem.tell(new CreateServiceJob(serviceId), getSelf());
			}
		});
	}
	
	private CompletableFuture<Boolean> createImportJob(String datasetId) {
		log.debug("requesting to refresh dataset: {}", datasetId);
		
		return f.ask(jobSystem, new CreateImportJob(datasetId), Ack.class) .thenApply(msg -> true);
	}

	@Override
	protected void preStartAdmin() {
		onPut(Service.class, service -> createServiceJob(service.id()));
		
		onPut(Layer.class, layer -> createServiceJobsForLayer(layer.id()));
		onPut(LayerGroup.class, layer -> createServiceJobsForLayer(layer.id()));
		
		onDelete(Layer.class, 
			layerId -> f.ask(serviceManager, new GetServicesWithLayer(layerId), TypedIterable.class),		
			this::createServiceJobs);
		
		onDelete(LayerGroup.class, 
			layerId -> f.ask(serviceManager, new GetServicesWithLayer(layerId), TypedIterable.class),		
			this::createServiceJobs);
		
		doQuery(RefreshDataset.class, refreshDataset -> createImportJob(refreshDataset.getDatasetId()));
	}
}
