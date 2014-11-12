package nl.idgis.publisher.job;

import nl.idgis.publisher.job.messages.GetHarvestJobs;
import nl.idgis.publisher.job.messages.GetImportJobs;
import nl.idgis.publisher.job.messages.GetServiceJobs;
import nl.idgis.publisher.job.messages.JobManagerRequest;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class JobSystem extends UntypedActor {
	
	private final ActorRef database, harvester, loader, service;
	
	private ActorRef jobManager;
	
	public JobSystem(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
		this.service = service;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service) {
		return Props.create(JobSystem.class, database, harvester, loader, service);
	}
	
	@Override
	public void preStart() throws Exception {
		jobManager = getContext().actorOf(
				JobManager.props(database), "manager");
		
		getContext().actorOf(
				Initiator.props()
					.add(harvester, "harvester", new GetHarvestJobs())
					.add(loader, "loader", new GetImportJobs())
					.add(service, "service", new GetServiceJobs())
					.create(jobManager), 
				"initiator");
		
		getContext().actorOf(Creator.props(jobManager, database), "creator");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof JobManagerRequest) {
			jobManager.forward(msg, getContext());
		} else {		
			unhandled(msg);
		}
	}

}
