package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.Service;

import nl.idgis.publisher.job.manager.messages.CreateServiceJob;
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

	@Override
	protected void preStartAdmin() {
		onPut(Service.class, service -> createServiceJob(service.id()));
		
		onPut(Layer.class, layer -> createServiceJobsForLayer(layer.id()));
		onPut(LayerGroup.class, layer -> createServiceJobsForLayer(layer.id()));
	}

}
