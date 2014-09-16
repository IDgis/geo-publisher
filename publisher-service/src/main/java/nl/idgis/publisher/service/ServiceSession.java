package nl.idgis.publisher.service;

import java.util.HashMap;
import java.util.Map;

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
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import akka.japi.Procedure;

public class ServiceSession extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ServiceJobInfo job;	
	private final ActorRef database;
	
	private final Map<String, String> connectionParameters;
	private final ServiceRest rest;
	
	public ServiceSession(ServiceJobInfo job, ActorRef database, String serviceLocation, String user, String password, Map<String, String> connectionParameters) throws Exception{
		this.job = job;
		
		this.database = database;
		this.connectionParameters = connectionParameters;
		
		rest = new ServiceRest(serviceLocation, user, password);
	}
	
	public static Props props(ServiceJobInfo job, ActorRef database, String serviceLocation, String user, String password, Map<String, String> connectionParameters) {
		return Props.create(ServiceSession.class, job, database, serviceLocation, user, password, connectionParameters);
	}
	
	public void preStart() throws Exception {
		log.debug("starting job");
		
		database.tell(new UpdateJobState(job, JobState.STARTED), getSelf());
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Ack) {
			getContext().become(busy());			
			execute();			
		} else {
			onElseReceive(msg);
		}
	}	
	
	public Procedure<Object> busy() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				onElseReceive(msg);
			}			
		};
	}
	
	public Procedure<Object> waitingForAck() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("job completed");
					getContext().stop(getSelf());
				} else {
					onElseReceive(msg);
				}
			}			
		};
	}
	
	private void onElseReceive(Object msg) throws Exception {
		unhandled(msg);
	}
	
	private boolean hasTable(Workspace workspace, DataStore dataStore, String tableName) throws Exception {
		
		for(FeatureType featureType : rest.getFeatureTypes(workspace, dataStore)) {
			if(featureType.getNativeName().equals(tableName)) {
				return true;
			}
		}

		return false;
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
	
	private void execute() {
		log.debug("executing job");
		
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
		
		getContext().become(waitingForAck());
		
		log.debug("service job finalized: " + job);
	}
}
