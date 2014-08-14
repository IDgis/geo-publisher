package nl.idgis.publisher.service.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.ImportJob;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.loader.messages.SessionFinished;
import nl.idgis.publisher.service.loader.messages.SessionStarted;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

public class Loader extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef geometryDatabase, database, harvester;
	private Map<ImportJob, ActorRef> sessions;

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
		if(msg instanceof ImportJob) {
			handleImportJob((ImportJob)msg);
		} else if(msg instanceof SessionStarted) {
			handleSessionStarted((SessionStarted)msg);
		} else if(msg instanceof SessionFinished) {
			handleSessionFinished((SessionFinished)msg);				
		} else {
			unhandled(msg);
		}
	}

	private void handleSessionFinished(SessionFinished msg) {
		ImportJob importJob = msg.getImportJob();
		
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
		
		sessions.put(msg.getImportJob(), getSender());
		
		getSender().tell(new Ack(), getSelf());
	}

	private void handleImportJob(final ImportJob importJob) {
		log.debug("data import requested: " + importJob);
		
		final String dataSourceId = importJob.getDataSourceId();
		Patterns.ask(harvester, new GetDataSource(dataSourceId), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					if(msg instanceof NotConnected) {
						log.warning("not connected: " + dataSourceId);
					} else {
						final ActorRef dataSource = (ActorRef)msg;
						
						log.debug("dataSource received");
						
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
													importJob.getDatasetId(),  
													importJob.getColumns());						
											
											Patterns.ask(transaction, ct, 15000)
												.onSuccess(new OnSuccess<Object>() {

													@Override
													public void onSuccess(Object msg) throws Throwable {
														log.debug("table created");
														
														List<String> columnNames = new ArrayList<>();
														for(Column column : importJob.getColumns()) {
															columnNames.add(column.getName());
														}
														
														dataSource.tell(
																new GetDataset(
																		importJob.getSourceDatasetId(), 
																		columnNames, 
																		LoaderSession.props(
																				getSelf(),
																				importJob, 
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
				
			}, getContext().dispatcher());
	}	
}
