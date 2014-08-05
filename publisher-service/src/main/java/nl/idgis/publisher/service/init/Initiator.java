package nl.idgis.publisher.service.init;

import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import nl.idgis.publisher.database.messages.GetNextHarvestJob;
import nl.idgis.publisher.database.messages.GetNextImportJob;
import nl.idgis.publisher.database.messages.HarvestJob;
import nl.idgis.publisher.database.messages.NoJob;
import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.service.init.messages.Initiate;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Initiator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database, harvester, loader;
	
	public Initiator(ActorRef database, ActorRef harvester, ActorRef loader) {
		this.database = database;
		this.harvester = harvester;
		this.loader = loader;
	}
	
	public static Props props(ActorRef database, ActorRef harvester, ActorRef loader) {
		return Props.create(Initiator.class, database, harvester, loader);
	}
	
	@Override
	public void preStart() throws Exception {
		FiniteDuration interval = Duration.create(10, TimeUnit.SECONDS);
		getContext().system().scheduler().schedule(Duration.Zero(), interval, getSelf(), new Initiate(), getContext().dispatcher(), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Initiate) {
			log.debug("initiating actions");
			
			Patterns.ask(database, new GetNextHarvestJob(), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						if(msg instanceof NoJob) {
							log.debug("no harvest job pending");
						} else {						
							log.debug("harvest job received");
							
							HarvestJob harvestJob = (HarvestJob)msg;
							harvester.tell(new Harvest(harvestJob.getDataSourceId()), getSelf());
						}
					}					
				}, getContext().dispatcher());
			
			Patterns.ask(database, new GetNextImportJob(), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						if(msg instanceof NoJob) {
							log.debug("no import job pending");
						} else {						
							log.debug("import job received");
							loader.tell(msg, getSelf());
						}
					}
				}, getContext().dispatcher());			
		} else {
			unhandled(msg);
		}
	}
}
