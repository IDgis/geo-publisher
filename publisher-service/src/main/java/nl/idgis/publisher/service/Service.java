package nl.idgis.publisher.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.typesafe.config.Config;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.AbstractStateMachine;
import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;
import nl.idgis.publisher.messages.ActiveJob;
import nl.idgis.publisher.messages.ActiveJobs;
import nl.idgis.publisher.messages.GetActiveJobs;
import nl.idgis.publisher.service.rest.ServiceRest;

public class Service extends AbstractStateMachine<String> {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ServiceRest rest;
	
	private final Map<String, String> connectionParameters;

	public Service(String serviceLocation, String user, String password, Map<String, String> connectionParameters) throws Exception {		
		this.connectionParameters = Collections.unmodifiableMap(connectionParameters);
		
		rest = new ServiceRest(serviceLocation, user, password);
	}
	
	public static Props props(Config geoserverConfig, Config geometryDatabaseConfig) {
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
		
		return Props.create(Service.class, serviceLocation, user, password, connectionParameters);
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
	
	private void handleGetActiveJobs() {
		log.debug("no active job");
		
		getSender().tell(new ActiveJobs(Collections.<ActiveJob>emptyList()), getSelf());
	}	
	
	private void handleServiceJob(ServiceJobInfo job) {
		log.debug("executing service job: " + job);
		
		// TODO: implement service job handling
	}	

	@Override
	protected void timeout(String state) {
		log.debug("timeout during: " + state);
	}
}
