package nl.idgis.publisher.provider;

import java.io.Serializable;
import java.util.Optional;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.MessageProtocolActors;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;
import nl.idgis.publisher.protocol.messages.Hello;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class ClientActors extends MessageProtocolActors {
	
	private static class CreateProviders implements Serializable {
	
		private static final long serialVersionUID = 3102092189552667733L;
		
		private final ActorRef harvester;
		
		CreateProviders(ActorRef harvester) {
			this.harvester = harvester;
		}

		public ActorRef getHarvester() {
			return harvester;
		}

		@Override
		public String toString() {
			return "CreateProviders [harvester=" + harvester + "]";
		}
		
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Config config;
	
	public ClientActors(Config config) {
		this.config = config;
	}
	
	public static Props props(Config config) {
		return Props.create(ClientActors.class, config);
	}
	
	@Override
	public final void onReceiveElse(Object msg) {
		if(msg instanceof CreateProviders) {
			createProviders(((CreateProviders)msg).getHarvester());
		} else {
			unhandled(msg);
		}	
	}

	private void createProviders(ActorRef harvester) {
		log.debug("creating providers");
		
		ProviderPropsFactory factory = new ProviderPropsFactory(log);
		config.getConfigList("instances").stream()
			.map(factory::props)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.forEach(providerProps -> {
				String name = providerProps.getName();
				
				ActorRef provider = getContext().actorOf(
					providerProps.getProps(), 
					"provider-" + name);
				
				harvester.tell(new Hello(name), provider);
			});
	} 
	
	protected void createActors(ActorRef messageProtocolHandler, ActorRef messagePackagerProvider) {
		log.debug("creating client actors");
		
		getContext().actorOf(Admin.props(), "admin");

		ActorRef self = getSelf();
		f.ask(messagePackagerProvider, new GetMessagePackager("harvester", true), ActorRef.class).thenAccept(harvester -> {
			self.tell(new CreateProviders(harvester), self);
		});
	}
}
