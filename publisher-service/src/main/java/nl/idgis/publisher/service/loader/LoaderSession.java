package nl.idgis.publisher.service.loader;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.ImportJob;
import nl.idgis.publisher.database.messages.InsertRecord;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.domain.log.GenericEvent;
import nl.idgis.publisher.domain.log.ImportLogLine;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.service.loader.messages.GetCount;
import nl.idgis.publisher.service.loader.messages.SessionFinished;
import nl.idgis.publisher.service.loader.messages.SessionStarted;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class LoaderSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ImportJob importJob;
	private final ActorRef loader, geometryDatabase, database;
	
	private long count = 0;
	
	public LoaderSession(ActorRef loader, ImportJob importJob, ActorRef geometryDatabase, ActorRef database) {
		this.loader = loader;
		this.importJob = importJob;
		this.geometryDatabase = geometryDatabase;
		this.database = database;
	}
	
	public static Props props(ActorRef loader, ImportJob importJob, ActorRef geometryDatabase, ActorRef database) {
		return Props.create(LoaderSession.class, loader, importJob, geometryDatabase, database);
	}
	
	@Override
	public void preStart() throws Exception {
		Patterns.ask(loader, new SessionStarted(importJob), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("sessions started");
				}
				
			}, getContext().dispatcher());		
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Record) {			 			
			handleRecord((Record)msg);		
		} else if(msg instanceof Failure) {
			handleFailure((Failure)msg);
		} else if(msg instanceof End) {						
			handleEnd((End)msg);
		} else if(msg instanceof GetCount) {
			handleGetCount((GetCount)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleGetCount(GetCount msg) {
		getSender().tell(count, getSelf());		
	}

	private void handleEnd(final End msg) {
		log.info("import completed");
		
		Patterns.ask(geometryDatabase, new Commit(), 15000)				
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("transaction committed");
					
					ImportLogLine logLine = new ImportLogLine(GenericEvent.FINISHED, importJob.getDatasetId());
					Patterns.ask(database, new StoreLog(logLine), 15000)
						.onSuccess(new OnSuccess<Object>() {

							@Override
							public void onSuccess(Object msg) throws Throwable {
								log.debug("import finished: " + count);
								
								finalizeSession();
							}					
						}, getContext().dispatcher());
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
					
					finalizeSession();
				}
				
			}, getContext().dispatcher());
	}
	
	private void finalizeSession() {
		Patterns.ask(loader, new SessionFinished(importJob), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("session finalized");
					
					getContext().stop(getSelf());
				}
				
			}, getContext().dispatcher());
	}

	private void handleRecord(final Record record) {
		count++;
		
		log.debug("record received: " + record + " " + count);
		
		final ActorRef sender = getSender(), self = getSelf();
		Patterns.ask(geometryDatabase, new InsertRecord(
				importJob.getDatasetId(), 
				importJob.getColumns(), 
				record.getValues()), 15000)
				
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						sender.tell(new NextItem(), self);
					}
					
				}, getContext().dispatcher());
	}
}
