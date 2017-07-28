package nl.idgis.publisher.harvester.server;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.FiniteDuration;

import nl.idgis.publisher.harvester.sources.ProviderConnectionClient;
import nl.idgis.publisher.protocol.MessageProtocolActors;
import nl.idgis.publisher.protocol.messages.GetMessagePackager;
import nl.idgis.publisher.protocol.messages.GetTransferedTotal;
import nl.idgis.publisher.protocol.messages.TransferedTotal;

public class ServerActors extends MessageProtocolActors {
	
	private static final FiniteDuration TRANSFER_TOTAL_INTERVAL = FiniteDuration.create(30, TimeUnit.SECONDS); 
	
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

	private final Config harvesterConfig;
	
	public ServerActors(String harvesterName, ActorRef harvester, Config harvesterConfig) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
		this.harvesterConfig = harvesterConfig;
	}

	public static Props props(String harvesterName, ActorRef harvester, Config harvesterConfig) {
		return Props.create(ServerActors.class, harvesterName, harvester, harvesterConfig);
	}
	
	@Override
	public void onReceiveElse(Object msg) {
		if(msg instanceof CreateProviderConnectionClient) {
			createProviderConnectionClient(((CreateProviderConnectionClient)msg).getAdmin());
		} else if(msg instanceof TransferedTotal) {
			log.info(msg.toString());
		} else {
			unhandled(msg);
		}
	}
	
	private void createProviderConnectionClient(ActorRef admin) {
		log.debug("creating provider connection client");
		
		getContext().actorOf(ProviderConnectionClient.props(harvesterName, harvester, admin, harvesterConfig), "harvester");
	}

	@Override
	protected void createActors(ActorRef messageProtocolHandler, ActorRef messagePackagerProvider) {
		log.debug("creating server actors");
		
		getContext().system().scheduler().schedule(
				FiniteDuration.Zero(), 
				TRANSFER_TOTAL_INTERVAL, 
				messageProtocolHandler, 
				new GetTransferedTotal(), 
				getContext().dispatcher(), 
				getSelf());
		
		ActorRef self = getSelf();
		f.ask(messagePackagerProvider, new GetMessagePackager("admin", true), ActorRef.class).thenAccept(admin -> {
			self.tell(new CreateProviderConnectionClient(admin), self);
		});
	}
}
