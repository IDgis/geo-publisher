package nl.idgis.publisher.provider;

import nl.idgis.publisher.protocol.MessageProtocolActors;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;
import nl.idgis.publisher.protocol.messages.Hello;

import scala.concurrent.Future;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

import com.typesafe.config.Config;

public class ClientActors extends MessageProtocolActors {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	
	public ClientActors(Config config) {
		this.config = config;
	}
	
	public static Props props(Config config) {
		return Props.create(ClientActors.class, config);
	}
	
	protected void createActors(ActorRef messagePackagerProvider) {
		log.debug("creating client actors");
		
		
								
		Future<Object> harvesterPackager = Patterns.ask(messagePackagerProvider, new GetMessagePackager("harvester"), 1000);
		harvesterPackager.onSuccess(new OnSuccess<Object>() {

			@Override
			public void onSuccess(Object msg) {
				final ActorRef harvester = (ActorRef)msg;
				
				for(Config instance : config.getConfigList("instances")) {
					final String instanceName = instance.getString("name");
					
					final ActorRef provider = getContext().actorOf(Provider.props(instance, instanceName), "provider-" + instanceName);
					
					harvester.tell(new Hello(instanceName), provider);
				}
			}
		}, getContext().system().dispatcher());
	}
}
