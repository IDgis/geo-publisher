package nl.idgis.publisher.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import nl.idgis.publisher.AbstractSession;
import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.InsertRecord;
import nl.idgis.publisher.database.messages.Rollback;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Filter.FilterExpression;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.loader.messages.SessionStarted;
import nl.idgis.publisher.messages.GetProgress;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.messages.Timeout;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.database.Record;
import nl.idgis.publisher.provider.protocol.database.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;

import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.Futures;
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
	
	private long totalCount = 0, insertCount = 0, filteredCount = 0;
	
	public LoaderSession(ActorRef loader, ImportJobInfo importJob, ActorRef geometryDatabase, ActorRef database) throws IOException {
		this.loader = loader;
		this.importJob = importJob;
		this.geometryDatabase = geometryDatabase;
		this.database = database;
		
		String filterCondition = importJob.getFilterCondition();
		if(filterCondition == null)  {
			filterEvaluator = null;
		} else {
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectReader reader = objectMapper.reader(Filter.class);
			Filter filter = reader.readValue(filterCondition);
			
			FilterExpression expression = filter.getExpression();
			if(expression == null) {
				filterEvaluator = null;
			} else {
				filterEvaluator = new FilterEvaluator(importJob.getColumns(), expression);
			}
		}
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
		
		
		if(log.isDebugEnabled()) { // Record.toString() is rather expensive
			log.debug("record received: " + record + " " + (insertCount + filteredCount) + "/" + totalCount + " (filtered: " + filteredCount + ")");
		}
		
		if(filterEvaluator != null && !filterEvaluator.evaluate(record)) {
			filteredCount++;
			
			return Futures.successful((Object)new Ack());
		} else {
			insertCount++;
			
			return Patterns.ask(geometryDatabase, new InsertRecord(
					importJob.getCategoryId(),
					importJob.getDatasetId(), 
					importJob.getColumns(), 
					record.getValues()), 15000);
		}
				
				
	}
}
