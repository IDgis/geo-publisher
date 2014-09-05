package nl.idgis.publisher.loader;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.loader.messages.SessionStarted;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.messages.GetProgress;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.protocol.messages.Ack;

import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.pattern.Patterns;

public class Loader extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef geometryDatabase, database, harvester;
	private Map<ImportJobInfo, ActorRef> sessions;

	public Loader(ActorRef geometryDatabase, ActorRef database, ActorRef harvester) {
		this.geometryDatabase = geometryDatabase;
		this.database = database;
		this.harvester = harvester;
		
		sessions = new HashMap<>();
	}
	
	public static Props props(ActorRef geometryDatabase, ActorRef database, ActorRef harvester) {
		return Props.create(Loader.class, geometryDatabase, database, harvester);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ImportJobInfo) {
			handleImportJob((ImportJobInfo)msg);
		} else if(msg instanceof SessionStarted) {
			handleSessionStarted((SessionStarted)msg);
		} else if(msg instanceof SessionFinished) {
			handleSessionFinished((SessionFinished)msg);
		} else if(msg instanceof GetActiveJobs) {
			handleGetActiveJobs((GetActiveJobs)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleGetActiveJobs(GetActiveJobs msg) {
		Patterns.pipe(
			Futures.traverse(sessions.entrySet(), 
					new Function<Map.Entry<ImportJobInfo, ActorRef>, Future<ActiveJob>>() {
	
						@Override
						public Future<ActiveJob> apply(Entry<ImportJobInfo, ActorRef> entry) throws Exception {
							final ImportJobInfo importJob = entry.getKey();
							final ActorRef session = entry.getValue();
							
							log.debug("requesting progress, job: " + importJob + " session: " + session);
							
							return Patterns.ask(session, new GetProgress(), 15000)
								.map(new Mapper<Object, ActiveJob>() {
	
									@Override
									public ActiveJob apply(Object msg) {
										Progress progress = (Progress)msg;
										return new ActiveJob(importJob, progress);
									}								
									
								}, getContext().dispatcher());
						}
				
			}, getContext().dispatcher())
				.map(new Mapper<Iterable<ActiveJob>, ActiveJobs>() {
	
					@Override
					public ActiveJobs apply(Iterable<ActiveJob> activeJobs) {
						return new ActiveJobs(activeJobs);
					}
					
				}, getContext().dispatcher()), getContext().dispatcher())
					.pipeTo(getSender(), getSelf());
	}

	private void handleSessionFinished(SessionFinished msg) {
		ImportJobInfo importJob = msg.getImportJob();
		
		if(sessions.containsKey(importJob)) {
			log.debug("import job finished: " + importJob);
			
			sessions.remove(importJob);
			
			getSender().tell(new Ack(), getSelf());
		} else {
			log.error("unknown import job: " + importJob + " finished");
		}
	}

	private void handleSessionStarted(SessionStarted msg) {
		log.debug("data import session started: " + msg);
		
		sessions.put(msg.getImportJob(), msg.getSession());
		
		getSender().tell(new Ack(), getSelf());
	}
	
	private boolean isImporting(String dataSourceId) {
		for(ImportJobInfo job : sessions.keySet()) {
			if(job.getDataSourceId().equals(dataSourceId)) {
				return true;
			}
		}
		
		return false;
	}

	private void handleImportJob(final ImportJobInfo importJob) {
		log.debug("data import requested: " + importJob);
		
		getContext().actorOf(
			LoaderSessionInitiator.props(isImporting(importJob.getDataSourceId()), 
				importJob, getSender(), database, geometryDatabase, harvester));
	}
	
}
