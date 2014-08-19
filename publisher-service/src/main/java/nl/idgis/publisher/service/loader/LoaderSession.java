package nl.idgis.publisher.service.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.InsertRecord;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.provider.protocol.database.Records;
import nl.idgis.publisher.service.harvester.sources.messages.StartImport;
import nl.idgis.publisher.service.loader.messages.SessionFinished;
import nl.idgis.publisher.service.loader.messages.SessionStarted;
import nl.idgis.publisher.service.loader.messages.Timeout;
import nl.idgis.publisher.service.messages.GetProgress;
import nl.idgis.publisher.service.messages.Progress;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.pattern.Patterns;

public class LoaderSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ImportJobInfo importJob;
	private final ActorRef loader, geometryDatabase, database;
	
	private Cancellable timeoutCancellable;
	private long totalCount = 0, count = 0;
	
	public LoaderSession(ActorRef loader, ImportJobInfo importJob, ActorRef geometryDatabase, ActorRef database) {
		this.loader = loader;
		this.importJob = importJob;
		this.geometryDatabase = geometryDatabase;
		this.database = database;
	}
	
	public static Props props(ActorRef loader, ImportJobInfo importJob, ActorRef geometryDatabase, ActorRef database) {
		return Props.create(LoaderSession.class, loader, importJob, geometryDatabase, database);
	}
	
	@Override
	public void preStart() throws Exception {
		Patterns.ask(loader, new SessionStarted(importJob, getSelf()), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("sessions started");
					
					scheduleTimeout();
				}
				
			}, getContext().dispatcher());		
	}
	
	@Override
	public void postStop() {
		timeoutCancellable.cancel();
	}
	
	private void scheduleTimeout() {
		if(timeoutCancellable != null) {
			timeoutCancellable.cancel();
		}
		
		ActorSystem system = getContext().system();
		
		timeoutCancellable = system.scheduler().scheduleOnce(
				Duration.create(15, TimeUnit.SECONDS), 
				getSelf(), 
				new Timeout(), 
				system.dispatcher(), 
				getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		scheduleTimeout();
		
		if(msg instanceof StartImport) {
			handleStartImport((StartImport)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> importing() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				scheduleTimeout();
				
				if(msg instanceof Records) {			 			
					handleRecords((Records)msg);		
				} else if(msg instanceof Failure) {
					handleFailure((Failure)msg);
				} else if(msg instanceof End) {						
					handleEnd((End)msg);
				} else if(msg instanceof GetProgress) {
					handleGetProgress((GetProgress)msg);
				} else if(msg instanceof Timeout) {
					handleTimeout((Timeout)msg);
				} else {
					unhandled(msg);
				}
			}
		};
	}

	private void handleStartImport(StartImport msg) {
		log.info("starting import");
		
		totalCount = msg.getCount();
		
		getSender().tell(new Ack(), getSelf());		
		getContext().become(importing());
	}

	private void handleTimeout(Timeout msg) {
		log.debug("timeout while executing job: " + importJob);
		
		finalizeSession(JobState.ABORTED);
	}

	private void handleGetProgress(GetProgress msg) {
		log.debug("progress requested");
		
		getSender().tell(new Progress(count, totalCount), getSelf());		
	}

	private void handleEnd(final End msg) {
		log.info("import completed");
		
		Patterns.ask(geometryDatabase, new Commit(), 15000)				
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("transaction committed");
					
					finalizeSession(JobState.SUCCEEDED);
				}
				
			}, getContext().dispatcher());
	}

	private void handleFailure(final Failure failure) {
		log.error("import failed: " + failure.getCause());
		
		Patterns.ask(geometryDatabase, new Rollback(), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("transaction rolled back");
					
					finalizeSession(JobState.FAILED);
				}
				
			}, getContext().dispatcher());
	}
	
	private void finalizeSession(JobState state) {
		log.debug("finalizing session: " + state);
		
		Patterns.ask(database, new UpdateJobState(importJob, state), 15000)
		.onSuccess(new OnSuccess<Object>() {

			@Override
			public void onSuccess(Object msg) throws Throwable {
				Patterns.ask(loader, new SessionFinished(importJob), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						log.debug("session finalized");
						
						getContext().stop(getSelf());
					}
					
				}, getContext().dispatcher());		
			}					
		}, getContext().dispatcher());
	}
	
	private void handleRecords(Records msg) {
		List<Record> records = msg.getRecords();
		
		log.debug("records received: " + records.size());
		
		List<Future<Object>> futures = new ArrayList<>();
		for(Record record : records) {
			futures.add(handleRecord(record));
		}
		
		final ActorRef sender = getSender(), self = getSelf();
		Futures.sequence(futures, getContext().dispatcher())
			.onSuccess(new OnSuccess<Iterable<Object>>() {
	
				@Override
				public void onSuccess(Iterable<Object> msgs) throws Throwable {
					log.debug("records processed");
					
					sender.tell(new NextItem(), self);
				}
				
			}, getContext().dispatcher());
	}

	private Future<Object> handleRecord(final Record record) {
		count++;
		
		if(log.isDebugEnabled()) { // Record.toString() is rather expensive
			log.debug("record received: " + record + " " + count + "/" + totalCount);
		}
		
		return Patterns.ask(geometryDatabase, new InsertRecord(
				importJob.getCategoryId(),
				importJob.getDatasetId(), 
				importJob.getColumns(), 
				record.getValues()), 15000);
				
				
	}
}
