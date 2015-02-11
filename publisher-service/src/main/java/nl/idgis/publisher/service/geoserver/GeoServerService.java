package nl.idgis.publisher.service.geoserver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.protocol.messages.Ack;
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
	
	private Procedure<Object> provisioning(ActorRef initiator, ServiceJobInfo serviceJob) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof ServiceJobInfo) {
					// this shouldn't happen
					log.error("receiving service job while provisioning");
					getSender().tell(new Ack(), getSelf());
				} else if(msg instanceof GetActiveJobs) {
					getSender().tell(new ActiveJobs(Collections.singletonList(new ActiveJob(serviceJob))), getSelf());
				} else if(msg instanceof Terminated) {
					log.debug("provisioning completed");
					
					initiator.tell(new Ack(), getSelf());
					getContext().become(receive());
				} else {
					unhandled(msg);
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
		
		getContext().watch(provisioningService);
		serviceManager.tell(new GetService(serviceJob.getServiceId()), provisioningService);
		
		getContext().become(provisioning(getSender(), serviceJob));
	}
}
