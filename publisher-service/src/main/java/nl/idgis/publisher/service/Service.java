package nl.idgis.publisher.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import scala.concurrent.duration.Duration;

import com.typesafe.config.Config;

import nl.idgis.publisher.AbstractStateMachine;
import nl.idgis.publisher.database.messages.ServiceJobInfo;
import nl.idgis.publisher.database.messages.StoreLog;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobLog;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.job.LogLevel;
import nl.idgis.publisher.domain.job.service.ServiceLogType;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.rest.DataStore;
import nl.idgis.publisher.service.rest.FeatureType;
import nl.idgis.publisher.service.rest.ServiceRest;
import nl.idgis.publisher.service.rest.Workspace;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import akka.japi.Procedure;

public class Service extends AbstractStateMachine<String> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	private final ServiceRest rest;
	private final Map<String, String> connectionParameters;

	public Service(ActorRef database, String serviceLocation, String user, String password, Map<String, String> connectionParameters) throws Exception {
		this.database = database;
		this.connectionParameters = Collections.unmodifiableMap(connectionParameters);
		
		rest = new ServiceRest(serviceLocation, user, password);		
	}
	
	public static Props props(ActorRef database, Config geoserverConfig, Config geometryDatabaseConfig) {
		String serviceLocation = geoserverConfig.getString("url") + "rest/";
		String user = geoserverConfig.getString("user");
		String password = geoserverConfig.getString("password");		
		
		String url = geometryDatabaseConfig.getString("url");
		
		Pattern urlPattern = Pattern.compile("jdbc:postgresql://(.*):(.*)/(.*)");
		Matcher matcher = urlPattern.matcher(url);
		
		if(!matcher.matches()) {
			throw new IllegalArgumentException("incorrect database url");
		}
		
		Map<String, String> connectionParameters = new HashMap<>();
		connectionParameters.put("dbtype", "postgis");
		connectionParameters.put("host", matcher.group(1));
		connectionParameters.put("port", matcher.group(2));
		connectionParameters.put("database", matcher.group(3));
		connectionParameters.put("user", geometryDatabaseConfig.getString("user"));
		connectionParameters.put("passwd", geometryDatabaseConfig.getString("password"));
		
		return Props.create(Service.class, database, serviceLocation, user, password, connectionParameters);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ServiceJobInfo) {
			handleServiceJob((ServiceJobInfo)msg);
		} else if(msg instanceof GetActiveJobs) {
			handleGetActiveJobs();
		} else {		
			unhandled(msg);
		}
	}
	
	private void unhandled(ServiceJobInfo job, Object msg) {
		if(msg instanceof GetActiveJobs) {
			handleGetActiveJobs(job);
		} else {
			unhandled(msg);
		}
	}
	
	private void handleGetActiveJobs(ServiceJobInfo job) {
		log.debug("active job: " + job);
		
		getSender().tell(new ActiveJobs(Arrays.asList(new ActiveJob(job))), getSelf());
	}
	
	private void handleGetActiveJobs() {
		log.debug("no active job");
		
		getSender().tell(new ActiveJobs(Collections.<ActiveJob>emptyList()), getSelf());
	}

	private Pair<Workspace, DataStore> findDataStore(String schemaName) throws Exception {
		for(Workspace workspace : rest.getWorkspaces()) {
			log.debug("in workspace: " + workspace.getName());
			
			for(DataStore dataStore : rest.getDataStores(workspace)) {
				log.debug("in dataStore: " + dataStore.getName());
				
				Map<String, String> connectionParameters = dataStore.getConnectionParameters();
				if(connectionParameters.containsKey("schema")) {
					String currentSchemaName = connectionParameters.get("schema");
					
					if(schemaName.equals(currentSchemaName)) {
						return new Pair<>(workspace, dataStore);
					}
				}
			}
		}
		
		return null;
	}
	
	private Procedure<Object> waitingForJobCompletedStored(final ServiceJobInfo job, final ActorRef initiator) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("job completed");
					
					initiator.tell(new Ack(), getSelf());
					
					getContext().unbecome();
				} else {
					unhandled(job, msg);
				}
			}
			
		};
	}
	
	private Procedure<Object> waitingJobLogStored(final ServiceJobInfo job, final ActorRef initiator) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("verified job log stored"); 
					
					log.debug("service job succeeded: " + job);
					tellDatabaseDelayed(new UpdateJobState(job, JobState.SUCCEEDED));
					
					become("storing job completed state", waitingForJobCompletedStored(job, initiator));
				} else {
					unhandled(job, msg);
				}
			}
			
		};
	}
	
	private void tellDatabaseDelayed(Object msg) {
		getContext()
			.system()
			.scheduler()
				.scheduleOnce(
						Duration.create(10, TimeUnit.SECONDS), 
						database, 
						msg, 
						getContext().dispatcher(), 
						getSelf());
	}
	
	private Procedure<Object> waitingForJobStartedStored(final ActorRef initiator, final ServiceJobInfo job) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("job started");
					
					try {
						String schemaName = job.getSchemaName().toLowerCase();
						
						Workspace workspace;
						DataStore dataStore;
						
						Pair<Workspace, DataStore> result = findDataStore(schemaName);
						if(result == null) {
							workspace = new Workspace(schemaName);
							if(rest.addWorkspace(workspace)) {					
								Map<String, String> connectionParameters = new HashMap<>(Service.this.connectionParameters);
								connectionParameters.put("schema", schemaName);
								dataStore = new DataStore("publisher-geometry", connectionParameters);
								if(!rest.addDataStore(workspace, dataStore)) {
									throw new IllegalStateException("coulnd't create datastore");
								}
							} else {
								throw new IllegalStateException("couldn't create workspace");
							}
						} else {
							workspace = result.first();
							dataStore = result.second();
						}
						
						String tableName = job.getTableName().toLowerCase();
						if(hasTable(workspace, dataStore, tableName)) {
							log.debug("feature type for table already exists");
							
							database.tell(new StoreLog(job, JobLog.create(LogLevel.INFO, ServiceLogType.VERIFIED)), getSelf());
							become("storing verified job log", waitingJobLogStored(job, initiator));
						} else {
							log.debug("creating new feature type");
							
							if(!rest.addFeatureType(workspace, dataStore, new FeatureType(tableName))) {
								tellDatabaseDelayed(new UpdateJobState(job, JobState.FAILED));						
								become("storing job completed state", waitingForJobCompletedStored(job, initiator));
							} else {
								database.tell(new StoreLog(job, JobLog.create(LogLevel.INFO, ServiceLogType.ADDED)), getSelf());
								become("storing add job log", waitingJobLogStored(job, initiator));
							}
						}
					} catch(Exception e) {
						log.error(e, "service job failed: " + job);
						
						tellDatabaseDelayed(new UpdateJobState(job, JobState.FAILED));						
						become("storing job completed state", waitingForJobCompletedStored(job, initiator));
					}					
				} else {
					unhandled(job, msg);
				}
			}
			
		};
	}
	
	private void handleServiceJob(ServiceJobInfo job) {
		log.debug("executing service job: " + job);
		
		database.tell(new UpdateJobState(job, JobState.STARTED), getSelf());
		become("storing started job state", waitingForJobStartedStored(getSender(), job), false);
	}	

	private boolean hasTable(Workspace workspace, DataStore dataStore, String tableName) throws Exception {
					
		for(FeatureType featureType : rest.getFeatureTypes(workspace, dataStore)) {
			if(featureType.getNativeName().equals(tableName)) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected void timeout(String state) {
		log.debug("timeout during: " + state);
	}
}
