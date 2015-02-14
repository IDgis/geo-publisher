package nl.idgis.publisher.admin;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.domain.web.Service;

import nl.idgis.publisher.job.manager.messages.CreateServiceJob;

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

	@Override
	protected void preStartAdmin() {
		onPut(Service.class, service -> createServiceJob(service.id()));
	}

}
