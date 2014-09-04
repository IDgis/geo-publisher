package nl.idgis.publisher.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import scala.concurrent.duration.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import nl.idgis.publisher.database.messages.AddNotification;
import nl.idgis.publisher.database.messages.CreateTable;
import nl.idgis.publisher.database.messages.DatasetStatusInfo;
import nl.idgis.publisher.database.messages.GetDatasetStatus;
import nl.idgis.publisher.database.messages.ImportJobInfo;
import nl.idgis.publisher.database.messages.RemoveNotification;
import nl.idgis.publisher.database.messages.StartTransaction;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.ConfirmNotificationResult;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.Notification;
import nl.idgis.publisher.domain.job.NotificationResult;
import nl.idgis.publisher.domain.job.NotificationType;
import nl.idgis.publisher.domain.job.load.ImportNotificationType;
import nl.idgis.publisher.domain.service.Column;
import nl.idgis.publisher.domain.web.Filter;
import nl.idgis.publisher.domain.web.Filter.FilterExpression;
import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.messages.NotConnected;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.messages.Timeout;
import nl.idgis.publisher.protocol.messages.Ack;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class LoaderSessionInitiator extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final ImportJobInfo importJob;
	
	private final boolean dataSourceBusy;
	private final ActorRef initiator, database, geometryDatabase, harvester;
	
	private boolean acknowledged = false;
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
			handleDatasetStatusInfo((DatasetStatusInfo)msg);
		} else {
			unhandled(msg);
		}
	}

	private void handleDatasetStatusInfo(DatasetStatusInfo datasetStatus) {
		log.debug("dataset status received");
		
		if(datasetStatus.isSourceDatasetColumnsChanged()) {
			log.debug("source columns changed");
			
			if(importJob.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED)) {
				log.debug("notification already present");
			} else {
				log.debug("notification not present -> add it");
				database.tell(new AddNotification(importJob, ImportNotificationType.SOURCE_COLUMNS_CHANGED), getSelf());				
				become("adding notification", waitingForNotificationStored(false));
				return;
			}
		} else {
			log.debug("source columns not changed");
			
			if(importJob.hasNotification(ImportNotificationType.SOURCE_COLUMNS_CHANGED)) {
				log.debug("notification present -> remove it");
				database.tell(new RemoveNotification(importJob, ImportNotificationType.SOURCE_COLUMNS_CHANGED), getSelf());				
				become("removing notification", waitingForNotificationStored(true));
				return;
			} else {
				log.debug("notification not present");
			}
		}
		
		handleNotifications();
	}

	private void handleNotifications() {
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
					become("storing new started job state", waitingForJobStartedStateStored());
				}
			}
			
		};
	}
	
	private Procedure<Object> waitingForNotificationStored(final boolean continueImport) {
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
		
		log.debug("requesting columns: " + columnNames);
		
		dataSource.tell(
				new GetDataset(
						importJob.getSourceDatasetId(), 
						columnNames, 
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
					
					geometryDatabase.tell(new StartTransaction(), getSelf());
					become("starting transaction", waitingForTransactionCreated());
				} else {
					unhandled(msg);
				}
			}
			
		};
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
