package nl.idgis.publisher.loader;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.InsertRecord;
import nl.idgis.publisher.database.messages.Rollback;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;

import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.ImportJobInfo;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.provider.protocol.Record;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Stop;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.utils.FutureUtils;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure; 

public class LoaderSession extends UntypedActor {
	
	private static final FiniteDuration DEFAULT_RECEIVE_TIMEOUT = Duration.apply(30, TimeUnit.SECONDS);

	private static class FinalizeSession implements Serializable {

		private static final long serialVersionUID = -6298981994732740388L;
		
		private final JobState jobState;
		
		FinalizeSession(JobState jobState) {
			this.jobState = jobState;
		}
		
		JobState getJobState() {
			return this.jobState;
		}

		@Override
		public String toString() {
			return "FinalizeSession [jobState=" + jobState + "]";
		}		
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ImportJobInfo importJob;
	
	private final ActorRef loader, transaction, jobContext;
	
	private final FilterEvaluator filterEvaluator;
	
	private final Duration receiveTimeout;
	
	private long totalCount = 0, insertCount = 0, filteredCount = 0;
	
	private FutureUtils f;
	
	public LoaderSession(Duration receiveTimeout, ActorRef loader, ImportJobInfo importJob, FilterEvaluator filterEvaluator, ActorRef transaction, ActorRef jobContext) throws IOException {		
		this.receiveTimeout = receiveTimeout;
		this.loader = loader;
		this.importJob = importJob;
		this.filterEvaluator = filterEvaluator;
		this.transaction = transaction;
		this.jobContext = jobContext;
	}
	
	public static Props props(Duration receiveTimeout, ActorRef loader, ImportJobInfo importJob, FilterEvaluator filterEvaluator, ActorRef transaction, ActorRef jobContext) {
		return Props.create(LoaderSession.class, receiveTimeout, loader, importJob, filterEvaluator, transaction, jobContext);
	}
	
	public static Props props(ActorRef loader, ImportJobInfo importJob, FilterEvaluator filterEvaluator, ActorRef transaction, ActorRef jobContext) {
		return props(DEFAULT_RECEIVE_TIMEOUT, loader, importJob, filterEvaluator, transaction, jobContext);
	}
	
	@Override
	public final void preStart() throws Exception {
		getContext().setReceiveTimeout(receiveTimeout);
		
		f = new FutureUtils(getContext().dispatcher());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof StartImport) {
			handleStartImport((StartImport)msg);
		} else {
			onReceiveElse(msg);
		}
	}
	
	private void onReceiveElse(Object msg) {
		if(msg instanceof ReceiveTimeout) {				
			handleTimeout();
		} else if(msg instanceof FinalizeSession) {
			handleFinalizeSession((FinalizeSession)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> importing() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Records) {			 			
					handleRecords((Records)msg);		
				} else if(msg instanceof Failure) {
					handleFailure((Failure)msg);
				} else if(msg instanceof End) {						
					handleEnd((End)msg);
				} else {
					onReceiveElse(msg);
				}
			}			
		};
	}

	private void handleStartImport(StartImport msg) {
		log.info("starting import");
		
		totalCount = msg.getCount();
		
		loader.tell(new Progress(0, totalCount), getSelf());
		
		// data source
		getSender().tell(new Ack(), getSelf());
		
		// session initiator
		msg.getInitiator().tell(new Ack(), getSelf());
				
		getContext().become(importing());
	}

	private void handleTimeout() {
		log.error("timeout while executing job: {}", importJob);
		
		getSelf().tell(new FinalizeSession(JobState.ABORTED), getSelf());
	}
	
	private void handleEnd(final End end) {
		log.info("import completed");
		
		f.ask(transaction, new Commit()).thenApply(msg -> {				
			log.debug("transaction committed");					
			
			return new FinalizeSession(JobState.SUCCEEDED);
		}).exceptionally(t -> {
			log.error("couldn't commit transaction: {}", t);
			
			return new FinalizeSession(JobState.FAILED);
		}).thenAccept(msg -> {
			getSelf().tell(msg, getSelf());
		});
	}

	private void handleFailure(final Failure failure) {
		log.error("import failed: {}", failure.getCause());
		
		f.ask(transaction, new Rollback()).thenApply(msg -> {
			log.debug("transaction rolled back");
												
			return new FinalizeSession(JobState.FAILED);
		}).exceptionally(t -> {
			log.error("couldn't rollback transaction: {}", t);
			
			return new FinalizeSession(JobState.FAILED);
		}).thenAccept(msg -> {
			getSelf().tell(msg, getSelf());
		});	
	}
	
	private void handleFinalizeSession(FinalizeSession finalizeSession) {
		JobState state = finalizeSession.getJobState();
		
		log.debug("finalizing session: {}",  state);
		
		f.ask(jobContext, new UpdateJobState(state)).whenComplete((msg0, t0) -> {
			if(t0 != null) {
				log.error("couldn't change job state: {}", t0);
			} 
			
			f.ask(loader, new SessionFinished(importJob)).whenComplete((msg1, t1) -> {
				if(t1 != null) {
					log.error("couldn't finish import session: {}", t1);
				}
				
				log.debug("session finalized");
				
				getContext().stop(getSelf());
			});
		});
	}
	
	private void handleRecords(Records msg) {
		List<Record> records = msg.getRecords();
		
		log.debug("records received: {}", records.size());
		
		List<CompletableFuture<Object>> futures = new ArrayList<>();
		for(Record record : records) {
			futures.add(handleRecord(record));
		}
		
		final ActorRef sender = getSender();
		f.sequence(futures).whenComplete((results, t) -> {
			if(t != null) {
				log.error("exception waiting results: {}", t);
				
				getSelf().tell(new FinalizeSession(JobState.FAILED), getSelf());
			} else {
				for(Object result : results) {
					if(result instanceof Failure) {
						log.error("handle record failed: {}", result);
						
						sender.tell(new Stop(), getSelf());
						getSelf().tell(new FinalizeSession(JobState.FAILED), getSelf());
						
						return;
					}
				}
				
				log.debug("records processed");
				
				loader.tell(new Progress(insertCount + filteredCount, totalCount), getSelf());					
				sender.tell(new NextItem(), getSelf());
			}
		});		
	}

	private CompletableFuture<Object> handleRecord(final Record record) {
		
		log.debug("record received: {} {}/{} (filtered:{})", record, (insertCount + filteredCount), totalCount,  filteredCount);		
		
		if(filterEvaluator != null && !filterEvaluator.evaluate(record)) {
			filteredCount++;
			
			return f.successful((Object)new Ack());
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
			
			return f.ask(transaction, new InsertRecord(
					importJob.getCategoryId(),
					importJob.getDatasetId(), 
					columns, 
					values))
						.exceptionally(t -> new Failure(t))
						.thenApply(msg -> {
							if(msg instanceof Failure) {
								log.debug("failed to insert record: {}", record);
								
								return msg;
							} else {							
								return new Ack();
							}
						});
		}
	}
}
