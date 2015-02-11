package nl.idgis.publisher.service.geoserver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.geoserver.messages.EnsureFeatureType;
import nl.idgis.publisher.service.geoserver.messages.EnsureGroup;
import nl.idgis.publisher.service.geoserver.messages.EnsureWorkspace;
import nl.idgis.publisher.service.geoserver.messages.Ensured;
import nl.idgis.publisher.service.geoserver.messages.FinishEnsure;
import nl.idgis.publisher.service.geoserver.rest.DefaultGeoServerRest;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRest;
import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.utils.UniqueNameGenerator;

public class GeoServerService extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final ActorRef serviceManager;
	
	private final String serviceLocation, user, password;
	
	private final Map<String, String> connectionParameters;
	
	private GeoServerRest rest;

	public GeoServerService(ActorRef serviceManager, String serviceLocation, String user, String password, Map<String, String> connectionParameters) throws Exception {		
		this.serviceManager = serviceManager;
		this.serviceLocation = serviceLocation;
		this.user = user;
		this.password = password;
		this.connectionParameters = Collections.unmodifiableMap(connectionParameters);
	}
	
	public static Props props(ActorRef serviceManager, Config geoserverConfig, Config databaseConfig) {
		String serviceLocation = geoserverConfig.getString("url") + "rest/";
		String user = geoserverConfig.getString("user");
		String password = geoserverConfig.getString("password");		
		
		String url = databaseConfig.getString("url");
		
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
		connectionParameters.put("user", databaseConfig.getString("user"));
		connectionParameters.put("passwd", databaseConfig.getString("password"));
		
		return Props.create(GeoServerService.class, serviceManager, serviceLocation, user, password, connectionParameters);
	}
	
	@Override
	public void preStart() throws Exception {
		rest = new DefaultGeoServerRest(serviceLocation, user, password);
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ServiceJobInfo) {
			handleServiceJob((ServiceJobInfo)msg);
		} else if(msg instanceof GetActiveJobs) {
			getSender().tell(new ActiveJobs(Collections.<ActiveJob>emptyList()), getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	private void elseProvisioning(Object msg, ServiceJobInfo serviceJob) {
		if(msg instanceof ServiceJobInfo) {
			// this shouldn't happen
			log.error("receiving service job while provisioning");
			getSender().tell(new Ack(), getSelf());
		} else if(msg instanceof GetActiveJobs) {
			getSender().tell(new ActiveJobs(Collections.singletonList(new ActiveJob(serviceJob))), getSelf());
		} else{
			unhandled(msg);
		}
	}
	
	private void ensured() {
		log.debug("ensured");
		
		getSender().tell(new Ensured(), getSelf());
	}
	
	private Procedure<Object> group(ServiceJobInfo serviceJob) {
		return group(0, serviceJob);
	}
	
	private Procedure<Object> group(int depth, ServiceJobInfo serviceJob) {
		log.debug("-> group {}", depth);
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof EnsureGroup) {
					ensured();
					getContext().become(group(depth + 1, serviceJob), false);
				} else if(msg instanceof EnsureFeatureType) {
					ensured();
				} else if(msg instanceof FinishEnsure) {
					ensured();
					
					log.debug("unbecome group {}", depth);
					getContext().unbecome();
				} else {
					elseProvisioning(msg, serviceJob);
				}
			}				
		};
	}
	
	private Procedure<Object> root(ActorRef initiator, ServiceJobInfo serviceJob) {
		log.debug("-> root");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof EnsureGroup) {
					ensured();
					getContext().become(group(serviceJob), false);
				} else if(msg instanceof EnsureFeatureType) {
					ensured();
				} else if(msg instanceof FinishEnsure) {
					ensured();
					
					log.debug("ack job");
					initiator.tell(new Ack(), getSelf());
					getContext().unbecome();					
				} else {
					elseProvisioning(msg, serviceJob);
				}
			}
		};
	}
	
	private Procedure<Object> provisioning(ActorRef initiator, ServiceJobInfo serviceJob) {
		log.debug("-> provisioning");
		
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof EnsureWorkspace) {
					ensured();
					getContext().become(root(initiator, serviceJob), false);
				} else {
					elseProvisioning(msg, serviceJob);
				}
			}
			
		};
	}

	@Override
	public void postStop() {
		try {
			rest.close();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}	
	
	private void handleServiceJob(ServiceJobInfo serviceJob) {
		log.debug("executing service job: " + serviceJob);
		
		ActorRef provisioningService = getContext().actorOf(
				ProvisionService.props(), 
				nameGenerator.getName(ProvisionService.class));
		
		serviceManager.tell(new GetService(serviceJob.getServiceId()), provisioningService);
		
		getContext().become(provisioning(getSender(), serviceJob));
	}
}
