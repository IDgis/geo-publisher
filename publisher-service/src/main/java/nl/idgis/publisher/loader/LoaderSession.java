package nl.idgis.publisher.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import nl.idgis.publisher.AbstractSession;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.InsertRecord;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.UpdateJobState;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;

import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.messages.GetProgress;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.messages.Timeout;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;

import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import akka.pattern.Patterns;

public class LoaderSession extends AbstractSession {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ImportJobInfo importJob;
	private final ActorRef loader, geometryDatabase, database;
	
	private final FilterEvaluator filterEvaluator;
	
	private boolean inFailure = false;
	private long totalCount = 0, insertCount = 0, filteredCount = 0;
	
	public LoaderSession(ActorRef loader, ImportJobInfo importJob, FilterEvaluator filterEvaluator, ActorRef geometryDatabase, ActorRef database) throws IOException {		
		this.loader = loader;
		this.importJob = importJob;
		this.filterEvaluator = filterEvaluator;
		this.geometryDatabase = geometryDatabase;
		this.database = database;
	}
	
	public static Props props(ActorRef loader, ImportJobInfo importJob, FilterEvaluator filterEvaluator, ActorRef geometryDatabase, ActorRef database) {
		return Props.create(LoaderSession.class, loader, importJob, filterEvaluator, geometryDatabase, database);
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
		
		// data source
		getSender().tell(new Ack(), getSelf());
		
		// session initiator
		msg.getInitiator().tell(new Ack(), getSelf());
		
				
		getContext().become(importing());
	}

	private void handleTimeout(Timeout msg) {
		log.debug("timeout while executing job: " + importJob);
		
		finalizeSession(JobState.ABORTED);
	}

	private void handleGetProgress(GetProgress msg) {
		log.debug("progress requested");
		
		getSender().tell(new Progress(insertCount + filteredCount, totalCount), getSelf());		
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
		
		if(!inFailure) {
			inFailure = true;
		
			Patterns.ask(geometryDatabase, new Rollback(), 15000)
				.onComplete(new OnComplete<Object>() {
	
					@Override
					public void onComplete(Throwable t, Object msg) throws Throwable {
						if(t != null) {
							log.warning("rollback failed");
						} else {					
							log.debug("transaction rolled back");
						}
						
						finalizeSession(JobState.FAILED);
					}
					
				}, getContext().dispatcher());
		}
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
		
		final ActorRef sender = getSender();
		Futures.sequence(futures, getContext().dispatcher())
			.onSuccess(new OnSuccess<Iterable<Object>>() {
	
				@Override
				public void onSuccess(Iterable<Object> msgs) throws Throwable {
					log.debug("records processed");
					
					sender.tell(new NextItem(), getSelf());
				}
				
			}, getContext().dispatcher());
	}

	private Future<Object> handleRecord(final Record record) {
		
		
		if(log.isDebugEnabled()) { // Record.toString() is rather expensive
			log.debug("record received: " + record + " " + (insertCount + filteredCount) + "/" + totalCount + " (filtered: " + filteredCount + ")");
		}
		
		if(filterEvaluator != null && !filterEvaluator.evaluate(record)) {
			filteredCount++;
			
			return Futures.successful((Object)new Ack());
		} else {
			insertCount++;
			
			List<Object> recordValues = record.getValues();
			List<Column> columns = importJob.getColumns();			
			
			List<Object> values;
			if(recordValues.size() > columns.size()) {
				log.debug("creating smaller value list");
				
				values = new ArrayList<>(columns.size());
				
				Iterator<Object> valueItr = recordValues.iterator();
				for(int i = 0; i< columns.size(); i++) {
					values.add(valueItr.next());
				}
			} else {
				log.debug("use value list from source record");
				
				values = recordValues;
			}
			
			Future<Object> insertResult = Patterns.ask(geometryDatabase, new InsertRecord(
					importJob.getCategoryId(),
					importJob.getDatasetId(), 
					columns, 
					values), 15000);
			
			insertResult.onComplete(new OnComplete<Object>() {

				@Override
				public void onComplete(Throwable t, Object msg) throws Throwable {
					if(t != null) {
						log.debug("exception during insert record");
						
						getSelf().tell(new Failure(t), getSelf());
					}
					
					if(msg instanceof Failure) {
						log.debug("failed to insert record");
						
						getSelf().tell(msg, getSelf());
					}
				}
				
			}, getContext().dispatcher());
			
			return insertResult;
		}
				
				
	}
}
