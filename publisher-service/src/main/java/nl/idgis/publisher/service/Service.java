package nl.idgis.publisher.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.typesafe.config.Config;

import nl.idgis.publisher.database.messages.ServiceJobInfo;
import nl.idgis.publisher.database.messages.UpdateJobState;
import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.rest.DataStore;
import nl.idgis.publisher.service.rest.FeatureType;
import nl.idgis.publisher.service.rest.ServiceRest;
import nl.idgis.publisher.service.rest.Workspace;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import akka.pattern.Patterns;

public class Service extends UntypedActor {
	
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
		} else {		
			unhandled(msg);
		}
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
	
	private void handleServiceJob(final ServiceJobInfo job) {
		log.debug("executing service job: " + job);
		
		final ActorRef sender = getSender();
		Patterns.ask(database, new UpdateJobState(job, JobState.STARTED), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					log.debug("service job started: " + job);
					
					sender.tell(new Ack(), getSelf());
					execute(job);
				}
				
			}, getContext().dispatcher());
	}

	private void execute(ServiceJobInfo job) {
		try {
			String schemaName = job.getSchemaName();
			
			Workspace workspace;
			DataStore dataStore;
			
			Pair<Workspace, DataStore> result = findDataStore(schemaName);
			if(result == null) {
				workspace = new Workspace(schemaName);
				if(rest.addWorkspace(workspace)) {					
					Map<String, String> connectionParameters = new HashMap<>(this.connectionParameters);
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
			
			String tableName = job.getTableName();
			if(hasTable(workspace, dataStore, tableName)) {
				log.debug("feature type for table already exists");
			} else {
				log.debug("creating new feature type");
				
				if(!rest.addFeatureType(workspace, dataStore, new FeatureType(tableName))) {
					throw new IllegalStateException("couldn't create feature type");
				}
			}
			
			log.debug("service job succeeded: " + job);
			database.tell(new UpdateJobState(job, JobState.SUCCEEDED), getSelf());
		} catch(Exception e) {
			log.error(e, "service job failed: " + job);
			
			database.tell(new UpdateJobState(job, JobState.FAILED), getSelf());
		}
		
		log.debug("service job finalized: " + job);
	}

	private boolean hasTable(Workspace workspace, DataStore dataStore, String tableName) throws Exception {
					
		for(FeatureType featureType : rest.getFeatureTypes(workspace, dataStore)) {
			if(featureType.getNativeName().equals(tableName)) {
				return true;
			}
		}

		return false;
	}	
}
