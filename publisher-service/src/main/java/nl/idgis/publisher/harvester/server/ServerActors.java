package nl.idgis.publisher.harvester.server;

import java.io.Serializable;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.harvester.sources.ProviderConnectionClient;
import nl.idgis.publisher.protocol.MessageProtocolActors;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;

public class ServerActors extends MessageProtocolActors {
	
	private static class CreateProviderConnectionClient implements Serializable {

		private static final long serialVersionUID = -4919627204135337866L;
		
		private final ActorRef admin;
		
		CreateProviderConnectionClient(ActorRef admin) {
			this.admin = admin;
		}
		
		ActorRef getAdmin() {
			return admin;
		}

		@Override
		public String toString() {
			return "CreateProviderConnectionClient [admin=" + admin + "]";
		}
	}
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String harvesterName;
	
	private final ActorRef harvester;
	
	public ServerActors(String harvesterName, ActorRef harvester) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
	}

	public static Props props(String harvesterName, ActorRef harvester) {
		return Props.create(ServerActors.class, harvesterName, harvester);
	}
	
	@Override
	public void onReceiveElse(Object msg) {
		if(msg instanceof CreateProviderConnectionClient) {
			createProviderConnectionClient(((CreateProviderConnectionClient)msg).getAdmin());
		} else {
			unhandled(msg);
		}
	}
	
	private void createProviderConnectionClient(ActorRef admin) {
		log.debug("creating provider connection client");
		
		getContext().actorOf(ProviderConnectionClient.props(harvesterName, harvester, admin), "harvester");
	}

	@Override
	protected void createActors(ActorRef messageProtocolHandler, ActorRef messagePackagerProvider) {
		log.debug("creating server actors");
		
		ActorRef self = getSelf();
		f.ask(messagePackagerProvider, new GetMessagePackager("admin", true), ActorRef.class).thenAccept(admin -> {
			self.tell(new CreateProviderConnectionClient(admin), self);
		});
	}
}
