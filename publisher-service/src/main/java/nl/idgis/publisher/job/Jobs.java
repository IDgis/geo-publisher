package nl.idgis.publisher.job;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.messages.GetHarvestJobs;
import nl.idgis.publisher.database.messages.GetImportJobs;
import nl.idgis.publisher.database.messages.GetServiceJobs;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.util.Timeout;

public class Jobs extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester, loader, service;
	
	public Jobs(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
		this.service = service;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader, ActorRef service) {
		return Props.create(Jobs.class, database, harvester, loader, service);
	}
	
	private AsyncSQLQuery query() {
		return new AsyncSQLQuery(database, new Timeout(15, TimeUnit.SECONDS), getContext().dispatcher());
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().actorOf(
				Initiator.props()
					.add(harvester, new GetHarvestJobs())
					.add(loader, new GetImportJobs())
					.add(service, new GetServiceJobs())
					.create(getSelf()), 
				"initiator");
		
		getContext().actorOf(Creator.props(database), "creator");
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("jobs: " + msg);
		
		if(msg instanceof GetImportJobs) {
			database.forward(msg, getContext());
		} else if(msg instanceof GetHarvestJobs) {
			database.forward(msg, getContext());
		} else if(msg instanceof GetServiceJobs) {
			database.forward(msg, getContext());
		} else {
			unhandled(msg);
		}
	}
}
