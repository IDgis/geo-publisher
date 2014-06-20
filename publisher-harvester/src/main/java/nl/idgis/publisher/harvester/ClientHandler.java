package nl.idgis.publisher.harvester;

import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.protocol.Message;
import nl.idgis.publisher.protocol.metadata.EndOfList;
import nl.idgis.publisher.protocol.metadata.GetList;
import nl.idgis.publisher.protocol.metadata.Item;
import nl.idgis.publisher.protocol.metadata.NextItem;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;
import akka.japi.Procedure;

public class ClientHandler extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private ActorRef connection;

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.debug(msg.toString());
			
			connection = getSender();			
			connection.tell(new Message("provider", new Hello("My data harvester")), getSelf());
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
					
					connection.tell(new Message("metadata", new GetList()), getSelf());			
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
					
					getSender().tell(new Message("metadata", new NextItem()), getSelf());
				} else if(msg instanceof EndOfList) {
					log.debug("harvesting finished");
					
					getContext().unbecome();
				} else {
					defaultActions(msg);
				}
			}
		};
	}
}
