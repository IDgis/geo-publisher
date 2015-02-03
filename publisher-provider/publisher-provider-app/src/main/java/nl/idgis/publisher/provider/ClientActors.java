package nl.idgis.publisher.provider;

import java.io.File;
import java.io.Serializable;

import com.typesafe.config.Config;

import nl.idgis.publisher.protocol.MessageProtocolActors;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.database.Database;
import nl.idgis.publisher.provider.metadata.Metadata;

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
		
		for(Config instance : config.getConfigList("instances")) {
			final String instanceName = instance.getString("name");					
			
			final Props database = Database.props(instance.getConfig("database"), instanceName);
			final Props metadata = Metadata.props(new File(instance.getString("metadata.folder")));					
			
			final ActorRef provider = getContext().actorOf(Provider.props(database, metadata), instanceName);					
			harvester.tell(new Hello(instanceName), provider);
		}
	} 
	
	protected void createActors(ActorRef messagePackagerProvider) {
		log.debug("creating client actors");
		
		getContext().actorOf(Admin.props(), "admin");

		ActorRef self = getSelf();
		f.ask(messagePackagerProvider, new GetMessagePackager("harvester", true), ActorRef.class).thenAccept(harvester -> {
			self.tell(new CreateProviders(harvester), self);
		});
	}
}
