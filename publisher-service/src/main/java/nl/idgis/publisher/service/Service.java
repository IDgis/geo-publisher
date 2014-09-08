package nl.idgis.publisher.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.idgis.publisher.database.messages.ServiceJobInfo;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import com.typesafe.config.Config;

public class Service extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef database;
	private final Map<String, String> connectionParameters;
	private final String serviceLocation, user, password;

	public Service(ActorRef database, String serviceLocation, String user, String password, Map<String, String> connectionParameters) throws Exception {
		this.database = database;
		this.connectionParameters = Collections.unmodifiableMap(connectionParameters);		
		this.serviceLocation = serviceLocation;		
		this.user = user;
		this.password = password;
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
	
	
	
	private void handleServiceJob(final ServiceJobInfo job) {
		log.debug("executing service job: " + job);		
		getContext().actorOf(ServiceSession.props(job, database, serviceLocation, user, password, connectionParameters));
	}
	
}
