package nl.idgis.publisher.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import scala.concurrent.Future;

import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Filter.FilterExpression;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.loader.messages.SessionFinished;
import nl.idgis.publisher.loader.messages.SessionStarted;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.messages.GetProgress;
import nl.idgis.publisher.messages.Progress;
import nl.idgis.publisher.protocol.messages.Ack;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.dispatch.OnSuccess;
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
		
		final String dataSourceId = importJob.getDataSourceId();
		if(isImporting(dataSourceId)) {
			log.debug("already obtaining data from dataSource: " + dataSourceId);
		} else {		
			Patterns.ask(harvester, new GetDataSource(dataSourceId), 15000)
				.onSuccess(new OnSuccess<Object>() {
	
					@Override
					public void onSuccess(Object msg) throws Throwable {
						if(msg instanceof NotConnected) {
							log.warning("not connected: " + dataSourceId);
						} else {
							final ActorRef dataSource = (ActorRef)msg;
							
							log.debug("dataSource received");
							
							startImport(importJob, dataSource);
						}
					}
					
				}, getContext().dispatcher());
		}
	}

	private void startImport(final ImportJobInfo importJob, final ActorRef dataSource) {
		Patterns.ask(database, new UpdateJobState(importJob, JobState.STARTED), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("job started");
					
					Patterns.ask(geometryDatabase, new StartTransaction(), 15000)
					.onSuccess(new OnSuccess<Object>() {

						@Override
						public void onSuccess(Object msg) throws Throwable {
							TransactionCreated tc = (TransactionCreated)msg;
							log.debug("database transaction created");
							
							final ActorRef transaction = tc.getActor();
							
							CreateTable ct = new CreateTable(
									importJob.getCategoryId(),
									importJob.getDatasetId(),  
									importJob.getColumns());						
							
							Patterns.ask(transaction, ct, 15000)
								.onSuccess(new OnSuccess<Object>() {

									@Override
									public void onSuccess(Object msg) throws Throwable {
										log.debug("table created");
										
										List<Column> columns = new ArrayList<>();
										columns.addAll(importJob.getColumns());										
										
										FilterEvaluator filterEvaluator;
										String filterCondition = importJob.getFilterCondition();
										if(filterCondition == null)  {
											filterEvaluator = null;
											
											log.debug("no filter -> not filtering");
										} else {
											ObjectMapper objectMapper = new ObjectMapper();
											ObjectReader reader = objectMapper.reader(Filter.class);
											Filter filter = reader.readValue(filterCondition);
											
											FilterExpression expression = filter.getExpression();
											if(expression == null) {
												filterEvaluator = null;
												
												log.debug("empty filter -> not filtering");
											} else {
												for(Column column : FilterEvaluator.getRequiredColumns(expression)) {
													if(!columns.contains(column)) {
														log.debug("querying additional column for filter: " + column);
														columns.add(column);
													}
												}
												
												filterEvaluator = new FilterEvaluator(columns, expression);
												
												log.debug("filter evaluator constructed");
											}
										}
										
										List<String> columnNames = new ArrayList<>();
										for(Column column : columns) {
											columnNames.add(column.getName());
										}
										
										dataSource.tell(
												new GetDataset(
														importJob.getSourceDatasetId(), 
														columnNames, 
														LoaderSession.props(
																getSelf(),
																importJob,
																filterEvaluator,
																transaction, 
																database)), getSelf());
									}
																	
							}, getContext().dispatcher());
					}
				}, getContext().dispatcher());
			}
		}, getContext().dispatcher());
	}
}
