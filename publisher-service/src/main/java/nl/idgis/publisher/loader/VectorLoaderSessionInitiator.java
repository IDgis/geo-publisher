package nl.idgis.publisher.loader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.AsyncTransactionHelper;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;

import nl.idgis.publisher.domain.Log;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
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

import nl.idgis.publisher.dataset.messages.PrepareTable;
import nl.idgis.publisher.harvester.sources.messages.FetchVectorDataset;
import nl.idgis.publisher.job.context.messages.AddJobNotification;
import nl.idgis.publisher.job.context.messages.RemoveJobNotification;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.VectorImportJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.utils.FutureUtils;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class VectorLoaderSessionInitiator extends AbstractLoaderSessionInitiator<VectorImportJobInfo> {
	
	private final ActorRef database, datasetManager;
	
	private DatasetStatusInfo datasetStatus = null;
	
	private FilterEvaluator filterEvaluator = null;
	
	private List<Column> importColumns = null;
	
	private List<String> requestColumnNames = null;
	
	private Set<Column> missingColumns = null, missingFilterColumns = null;
	
	private boolean continueImport = true;
		
	protected FutureUtils f;
	
	protected AsyncDatabaseHelper db;
	
	public VectorLoaderSessionInitiator(VectorImportJobInfo importJob, ActorRef jobContext, ActorRef database, ActorRef datasetManager, Duration receiveTimeout) {
		super(importJob, jobContext, receiveTimeout);
				
		this.datasetManager = datasetManager;
		this.database = database;
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}
	
	public static Props props(VectorImportJobInfo importJob, ActorRef jobContext, ActorRef database, ActorRef datasetManager) {
		return props(importJob, jobContext, database, datasetManager, DEFAULT_RECEIVE_TIMEOUT);
	}
	
	public static Props props(VectorImportJobInfo importJob, ActorRef jobContext, ActorRef database, ActorRef datasetManager, Duration receiveTimeout) {
		return Props.create(VectorLoaderSessionInitiator.class, importJob, jobContext, database, datasetManager, receiveTimeout);
	}
	
	private Procedure<Object> waitingForDatasetStatusInfo() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof DatasetStatusInfo) {
					datasetStatus = (DatasetStatusInfo)msg;
					
					handleDatasetStatusInfo();
				} else {
					unhandled(msg);
				}
			}
			
		};
	}	

	private void handleDatasetStatusInfo() throws Exception {
		log.debug("dataset status received");
		
		if(datasetStatus.isSourceDatasetColumnsChanged()) {
			log.debug("source columns changed");
			
			if(importJob.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED)) {
				log.debug("notification already present");
			} else {
				log.debug("notification not present -> add it");
				jobContext.tell(new AddJobNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED), getSelf());
				
				continueImport = false;
				become("adding notification", waitingForNotificationStored());
				return;
			}
		} else {
			log.debug("source columns not changed");
			
			if(importJob.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED)) {
				log.debug("notification present -> remove it");
				jobContext.tell(new RemoveJobNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED), getSelf());				
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
		
		jobContext.tell(new UpdateJobState(JobState.STARTED), getSelf());
		become("storing started job state", waitingForJobStartedStored());		
	}
	
	private void prepareJob() throws Exception {		
		missingColumns = new HashSet<Column>();
		
		Set<Column> sourceDatasetColumns = new HashSet<Column>(importJob.getSourceDatasetColumns());
		
		for(Column column : importJob.getColumns()) {
			if(!sourceDatasetColumns.contains(column)) {
				missingColumns.add(column);
			}
		}
		
		missingFilterColumns = new HashSet<>();
		
		List<Column> requiredColumns = new ArrayList<>();
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
					if(!sourceDatasetColumns.contains(column)) {
						missingFilterColumns.add(column);
					}
					
					if(!requiredColumns.contains(column)) {
						log.debug("querying additional column for filter: " + column);
						requiredColumns.add(column);
					}
				}
				
				filterEvaluator = new FilterEvaluator(requiredColumns, expression);
				
				log.debug("filter evaluator constructed");
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
		
		importColumns = importJob.getColumns().stream()
			.filter(column -> !missingColumns.contains(column))
			.collect(Collectors.toList());
	}
	
	private Procedure<Object> waitingForJobFailedStored() { 
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					acknowledgeJobAndStop();
				} else {
					unhandled(msg);
				}
				
			}
		};
	}
	
	private Procedure<Object> waitingForLogsStored(final int totalLogCount) {
		return new Procedure<Object>() {
			
			int currentLogCount = 0;

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					currentLogCount++;
					
					if(currentLogCount == totalLogCount) {
						if(continueImport) {						
							startTransaction();
						} else {
							jobContext.tell(new UpdateJobState(JobState.FAILED), getSelf());
							
							become("storing failed job state", waitingForJobFailedStored());
						}
					}
				} else {
					unhandled(msg);
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
	
	private void startLoaderSession(AsyncTransactionHelper tx) throws Exception {
		log.debug("requesting columns: " + requestColumnNames);
		
		startLoaderSession(new FetchVectorDataset(
			importJob.getExternalSourceDatasetId(), 
			requestColumnNames, 
			VectorLoaderSession.props(								
				getContext().parent(), // loader
				importJob,
				importColumns,
				filterEvaluator,
				tx, 
				jobContext)));
	}
	
	private Procedure<Object> waitingForTablePrepared(AsyncTransactionHelper tx) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("table prepared");					
										
					startLoaderSession(tx);
				} else if(msg instanceof Failure) {
					log.error("table preparation failed: {}", msg);
					
					tx.rollback().thenRun(() -> {
						jobContext.tell(new Ack(), getSelf()); // TODO: feels redundant...
						jobContext.tell(new UpdateJobState(JobState.FAILED), getSelf());
						
						getContext().stop(getSelf());
					});
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private Procedure<Object> waitingForJobStartedStored() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("job started");
					
					prepareJob();
					
					int logCount = 0;
					if(!missingColumns.isEmpty()) {
						jobContext.tell(								
							Log.create(
									LogLevel.WARNING, 
									ImportLogType.MISSING_COLUMNS, 
									
									new MissingColumnsLog(
											importJob.getDatasetId(), 
											importJob.getDatasetName(), 
											missingColumns)), 
							getSelf());
						
						logCount++;						
					}
					
					if(!missingFilterColumns.isEmpty()) {
						jobContext.tell(								
							Log.create(
									LogLevel.ERROR, 
									ImportLogType.MISSING_FILTER_COLUMNS, 
									
									new MissingColumnsLog(
											importJob.getDatasetId(), 
											importJob.getDatasetName(), 
											missingFilterColumns)), 
							getSelf());
						
						continueImport = false;
						
						logCount++;	
					}
					
					if(logCount == 0) {
						startTransaction();
					} else {
						become("storing missing columns log", waitingForLogsStored(logCount));
					}
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private void startTransaction() {
		db.transaction().thenAccept(tx -> {
			datasetManager.tell(
				new PrepareTable(
					Optional.of(tx.getTransactionRef()),
					importJob.getDatasetId(), 
					importColumns), 
				getSelf());
			
			become("preparing table", waitingForTablePrepared(tx));
		});
		
	}
	
	protected void dataSourceReceived() {
		database.tell(new GetDatasetStatus(importJob.getDatasetId()), getSelf());
		become("retrieving dataset status info", waitingForDatasetStatusInfo());
	}
}
