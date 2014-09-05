package nl.idgis.publisher.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.database.messages.AddNotification;
import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.RemoveNotification;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.JobLog;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;
import nl.idgis.publisher.domain.job.load.ImportLogType;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.job.load.MissingColumnsLog;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Filter.FilterExpression;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.messages.Timeout;
import nl.idgis.publisher.protocol.messages.Ack;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class LoaderSessionInitiator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final ImportJobInfo importJob;
	
	private final boolean dataSourceBusy;
	private final ActorRef initiator, database, geometryDatabase, harvester;
	
	private DatasetStatusInfo datasetStatus = null;
	private FilterEvaluator filterEvaluator = null;
	private List<Column> requiredColumns = null;
	private List<String >requestColumnNames = null;
	private Set<Column> missingColumns = null;
	private boolean continueImport = true, acknowledged = false;
	
	private ActorRef dataSource, transaction;
	
	private Cancellable timeoutCancellable = null;	
	
	public LoaderSessionInitiator(boolean dataSourceBusy, ImportJobInfo importJob, ActorRef initiator, 
			ActorRef database, ActorRef geometryDatabase, ActorRef harvester) {
		
		this.dataSourceBusy = dataSourceBusy;
		this.importJob = importJob;
		this.initiator = initiator;
		this.database = database;
		this.geometryDatabase = geometryDatabase;
		this.harvester = harvester;
	}
	
	public static Props props(boolean dataSourceBusy, ImportJobInfo importJob, ActorRef initiator, ActorRef database, ActorRef geometryDatabase, ActorRef harvester) {
		return Props.create(LoaderSessionInitiator.class, dataSourceBusy, importJob, initiator, database, geometryDatabase, harvester);
	}
	
	@Override
	public void preStart() {
		database.tell(new GetDatasetStatus(importJob.getDatasetId()), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof DatasetStatusInfo) {
			datasetStatus = (DatasetStatusInfo)msg;
			
			handleDatasetStatusInfo();
		} else {
			unhandled(msg);
		}
	}

	private void handleDatasetStatusInfo() throws Exception {
		log.debug("dataset status received");
		
		if(datasetStatus.isSourceDatasetColumnsChanged()) {
			log.debug("source columns changed");
			
			if(importJob.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED)) {
				log.debug("notification already present");
			} else {
				log.debug("notification not present -> add it");
				database.tell(new AddNotification(importJob, ImportNotificationType.SOURCE_COLUMNS_CHANGED), getSelf());
				
				continueImport = false;
				become("adding notification", waitingForNotificationStored());
				return;
			}
		} else {
			log.debug("source columns not changed");
			
			if(importJob.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED)) {
				log.debug("notification present -> remove it");
				database.tell(new RemoveNotification(importJob, ImportNotificationType.SOURCE_COLUMNS_CHANGED), getSelf());				
				become("removing notification", waitingForNotificationStored());
				return;
			} else {
				log.debug("notification not present");
			}
		}
		
		handleNotifications();
	}

	private void handleNotifications() throws Exception {
		for(Notification notification : importJob.getNotifications()) {
			NotificationType<?> type = notification.getType();
			NotificationResult result = notification.getResult();
			
			if(ImportNotificationType.SOURCE_COLUMNS_CHANGED.equals(type)) {
				if(!ConfirmNotificationResult.OK.equals(result)) {
					log.debug("column changes not (yet) accepted");
					acknowledgeJobAndStop();
					return;
				}
			} else {
				log.error("unknown notification type: " + type.name());				
			}
		}
		
		requestDataSource();		
	}
	
	private void prepareJob() throws Exception {
		requiredColumns = new ArrayList<>();
		requiredColumns.addAll(importJob.getColumns());
		
		String filterCondition = importJob.getFilterCondition();
		if(filterCondition == null)  {
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
					if(!requiredColumns.contains(column)) {
						log.debug("querying additional column for filter: " + column);
						requiredColumns.add(column);
					}
				}
				
				filterEvaluator = new FilterEvaluator(requiredColumns, expression);
				
				log.debug("filter evaluator constructed");
			}
		}
		
		missingColumns = new HashSet<Column>();
		Set<Column> sourceDatasetColumns = new HashSet<Column>(importJob.getSourceDatasetColumns());
		
		for(Column requiredColumn : requiredColumns) {
			if(!sourceDatasetColumns.contains(requiredColumn)) {
				missingColumns.add(requiredColumn);
			}
		}
		
		requestColumnNames = new ArrayList<>();
		for(Column column : requiredColumns) {
			if(missingColumns.contains(column)) {
				log.debug("missing column removed: " + column);
			} else {
				requestColumnNames.add(column.getName());
			}
		}
	}
	
	private Procedure<Object> waitingForMissingColumnsLogStored() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					startTransaction();
				} else {
					unhandled(msg);
				}
			}			
		};
	}

	private void requestDataSource() {
		final String dataSourceId = importJob.getDataSourceId();		
		
		if(dataSourceBusy) {
			log.debug("already obtaining data from dataSource: " + dataSourceId);
			acknowledgeJobAndStop();
		} else {
			log.debug("fetching dataSource from harvester: " + dataSourceId);			
			harvester.tell(new GetDataSource(dataSourceId), getSelf());
			become("fetching dataSource from harvester", waitingForDataSource());
		}
	}
	
	private Procedure<Object> waitingForDataSource() {
		log.debug("waiting for harvester");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception { 
				if(msg instanceof NotConnected) {					
					log.warning("not connected: " + importJob.getDataSourceId());
					
					acknowledgeJobAndStop();
				} else if(msg instanceof ActorRef) {
					log.debug("dataSource received");
					
					dataSource = (ActorRef)msg;
					database.tell(new UpdateJobState(importJob, JobState.STARTED), getSelf());
					become("storing started job state", waitingForJobStartedStateStored());
				}
			}
			
		};
	}
	
	private Procedure<Object> waitingForNotificationStored() {
		log.debug("waiting for notification stored");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("notification stored");
					
					if(continueImport) {
						log.debug("continuing loader initialization");
						
						handleNotifications();
					} else {
						log.debug("stopping");
						
						acknowledgeJobAndStop();
					}
				} else {
					unhandled(msg);
				}
				
			}			
		};
	}
	
	private void startLoaderSession() throws IOException, JsonProcessingException {
		
		log.debug("requesting columns: " + requestColumnNames);
		
		dataSource.tell(
				new GetDataset(
						importJob.getSourceDatasetId(), 
						requestColumnNames, 
						LoaderSession.props(
								getContext().parent(),
								importJob,
								filterEvaluator,
								transaction, 
								database)), getSelf());
		
		getContext().stop(getSelf());
	}	
	
	private Procedure<Object> waitingForTableCreated() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("table created");
					
					startLoaderSession();
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private Procedure<Object> waitingForTransactionCreated() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof TransactionCreated) {
					log.debug("database transaction created");
					
					transaction = ((TransactionCreated) msg).getActor();
					
					CreateTable ct = new CreateTable(
							importJob.getCategoryId(),
							importJob.getDatasetId(),  
							importJob.getColumns());
					
					transaction.tell(ct, getSelf());
					become("creating table", waitingForTableCreated());
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private Procedure<Object> waitingForJobStartedStateStored() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("job started");
					
					acknowledgeJob();
					
					prepareJob();
					
					if(missingColumns.isEmpty()) {
						startTransaction();
					} else {
						MissingColumnsLog missingColumnsLog = new MissingColumnsLog(importJob.getDatasetId(), importJob.getDatasetName(), missingColumns);
						
						database.tell(new StoreLog(importJob, JobLog.create(LogLevel.INFO, ImportLogType.MISSING_COLUMNS, missingColumnsLog)), getSelf());
						become("storing missing columns log", waitingForMissingColumnsLogStored());
					}
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private void startTransaction() {
		geometryDatabase.tell(new StartTransaction(), getSelf());
		become("starting transaction", waitingForTransactionCreated());
	}
	
	private void scheduleTimeout() {
		if(timeoutCancellable != null ) {
			timeoutCancellable.cancel();
		}
		
		timeoutCancellable = getContext().system().scheduler()
				.scheduleOnce(					 
					Duration.create(15,  TimeUnit.SECONDS), 
					
					getSelf(), new Timeout(), 
					
					getContext().dispatcher(), getSelf());
	}

	
	private void become(final String stateMessage, final Procedure<Object> behavior) {
		
		scheduleTimeout();
		
		getContext().become(new Procedure<Object>() {			
			
			@Override
			public void apply(Object msg) throws Exception {
				scheduleTimeout();
				
				if(msg instanceof Timeout) {
					if(!acknowledged) {
						acknowledgeJob();
					}
					
					log.error("timeout during: " + stateMessage);
					
					getContext().stop(getSelf());
				} else {
					behavior.apply(msg);
				}
			}
			
		});
	}
	
	private void acknowledgeJobAndStop() {
		acknowledgeJob();
		getContext().stop(getSelf());
	}

	private void acknowledgeJob() {
		acknowledged = true;
		initiator.tell(new Ack(), getSelf());
	}
}
