package nl.idgis.publisher.loader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;

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

import nl.idgis.publisher.harvester.sources.messages.FetchVectorDataset;
import nl.idgis.publisher.job.context.messages.AddJobNotification;
import nl.idgis.publisher.job.context.messages.RemoveJobNotification;
import nl.idgis.publisher.job.context.messages.UpdateJobState;
import nl.idgis.publisher.job.manager.messages.VectorImportJobInfo;
import nl.idgis.publisher.protocol.messages.Ack;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Procedure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

public class VectorLoaderSessionInitiator extends AbstractLoaderSessionInitiator<VectorImportJobInfo> {
	
	private final ActorRef database;
	
	private DatasetStatusInfo datasetStatus = null;
	
	private FilterEvaluator filterEvaluator = null;
	
	private List<Column> requiredColumns = null;
	
	private List<String >requestColumnNames = null;
	
	private Set<Column> missingColumns = null, missingFilterColumns = null;
	
	private boolean continueImport = true;
	
	private ActorRef transaction;	
	
	public VectorLoaderSessionInitiator(VectorImportJobInfo importJob, ActorRef jobContext, ActorRef database) {
		super(importJob, jobContext);
				
		this.database = database;
	}
	
	public static Props props(VectorImportJobInfo importJob, ActorRef jobContext, ActorRef database) {
		return Props.create(VectorLoaderSessionInitiator.class, importJob, jobContext, database);
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
	
	private void startLoaderSession() throws Exception {
		log.debug("requesting columns: " + requestColumnNames);
		
		startLoaderSession(new FetchVectorDataset(
			importJob.getExternalSourceDatasetId(), 
			requestColumnNames, 
			VectorLoaderSession.props(								
				getContext().parent(), // loader
				importJob,
				filterEvaluator,
				transaction, 
				jobContext)));
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
							"staging_data",
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
		database.tell(new StartTransaction(), getSelf());
		become("starting transaction", waitingForTransactionCreated());
	}
	
	protected void dataSourceReceived() {
		database.tell(new GetDatasetStatus(importJob.getDatasetId()), getSelf());
		become("retrieving dataset status info", waitingForDatasetStatusInfo());
	}
}
