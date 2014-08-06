package nl.idgis.publisher.service.loader;

import java.util.ArrayList;
import java.util.List;

import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.ImportJob;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.domain.log.GenericEvent;
import nl.idgis.publisher.domain.log.ImportLogLine;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;

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

	public Loader(ActorRef geometryDatabase, ActorRef database, ActorRef harvester) {
		this.geometryDatabase = geometryDatabase;
		this.database = database;
		this.harvester = harvester;
	}
	
	public static Props props(ActorRef geometryDatabase, ActorRef database, ActorRef harvester) {
		return Props.create(Loader.class, geometryDatabase, database, harvester);
	}
	
	@Override
	public void preStart() throws Exception {
		
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ImportJob) {
			final ImportJob importJob = (ImportJob)msg;
			
			log.debug("data import requested: " + importJob);
			
			final String dataSourceId = importJob.getDataSourceId();
			Patterns.ask(harvester, new GetDataSource(dataSourceId), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						if(msg instanceof NotConnected) {
							log.warning("not connected: " + dataSourceId);
							
							ImportLogLine logLine = new ImportLogLine(GenericEvent.FINISHED, importJob.getDatasetId());
							database.tell(new StoreLog(logLine), getSelf());
						} else {
							final ActorRef dataSource = (ActorRef)msg;
							
							log.debug("dataSource received");
							
							ImportLogLine logLine = new ImportLogLine(GenericEvent.STARTED, importJob.getDatasetId());
							Patterns.ask(database, new StoreLog(logLine), 15000)
								.onSuccess(new OnSuccess<Object>() {

									@Override
									public void onSuccess(Object msg) throws Throwable {
										log.debug("started logline written");
										
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
		} else {
			unhandled(msg);
		}
	}	
}
