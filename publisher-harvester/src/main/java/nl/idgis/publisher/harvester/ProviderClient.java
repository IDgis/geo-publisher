package nl.idgis.publisher.harvester;

import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.protocol.metadata.EndOfList;
import nl.idgis.publisher.protocol.metadata.Failure;
import nl.idgis.publisher.protocol.metadata.GetList;
import nl.idgis.publisher.protocol.metadata.Item;
import nl.idgis.publisher.protocol.metadata.NextItem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;
import akka.japi.Procedure;

public class ProviderClient extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef provider, metadata;
	
	public ProviderClient(ActorRef provider, ActorRef metadata) {
		this.provider = provider;
		this.metadata = metadata;
	}
	
	public static Props props(ActorRef provider, ActorRef metadata) {
		return Props.create(ProviderClient.class, provider, metadata);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.debug(msg.toString());
			
			provider.tell(new Hello("My data harvester"), getSelf());
			getContext().become(active(), false);
		} else {
			defaultActions(msg);
		}
	}
	
	private void defaultActions(Object msg) {
		if(msg instanceof ConnectionClosed) {
			log.debug("disconnected");
			
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> active() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Harvest) {
					log.debug("harvesting started");
					
					metadata.tell(new GetList(), getSelf());			
					getContext().become(harvesting(), false);
				} else {
					defaultActions(msg);
				}
			}
		};
	}
	
	private Procedure<Object> harvesting() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Harvest) {
					log.debug("already harvesting");
				} else if(msg instanceof Item) {
					log.debug("item harvested: " + msg);
					
					metadata.tell(new NextItem(), getSelf());
				} else if(msg instanceof EndOfList) {
					log.debug("harvesting finished");
					
					getContext().unbecome();
				} else if(msg instanceof Failure) {
					log.error(msg.toString());
					
					getContext().unbecome();
				} else {
					defaultActions(msg);
				}
			}
		};
	}
}
