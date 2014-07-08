package nl.idgis.publisher.harvester;

import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.protocol.Hello;
import nl.idgis.publisher.protocol.database.FetchTable;
import nl.idgis.publisher.protocol.database.Record;
import nl.idgis.publisher.protocol.metadata.GetMetadata;
import nl.idgis.publisher.protocol.metadata.MetadataItem;
import nl.idgis.publisher.protocol.stream.End;
import nl.idgis.publisher.protocol.stream.Failure;
import nl.idgis.publisher.protocol.stream.NextItem;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;
import akka.japi.Procedure;

public class ProviderClient extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef metadata, database;
		
	public ProviderClient(ActorRef metadata, ActorRef database) {		
		this.metadata = metadata;
		this.database = database;
	}
	
	public static Props props(ActorRef metadata, ActorRef database) {
		return Props.create(ProviderClient.class, metadata, database);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.debug(msg.toString());
			
			getSender().tell(new Hello("My data harvester"), getSelf());
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
					
					metadata.tell(new GetMetadata(), getSelf());			
					getContext().become(harvesting(), false);
				} else {
					defaultActions(msg);
				}
			}
		};
	}
	
	private Procedure<Object> receivingData() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Record) {
					log.debug("record received");
					
					database.tell(new NextItem(), getSelf());
				} else if(msg instanceof End) {
					log.debug("data retrieval finished");					
					finish();
				} else if(msg instanceof Failure) {
					log.error("data retrieval failure: " + msg);
					finish();
				} else {
					unhandled(msg);
				}
			}	
			
			void finish() {
				metadata.tell(new NextItem(), getSelf());					
				getContext().unbecome();
			}
		};
	}
	
	private Procedure<Object> harvesting() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Harvest) {
					log.debug("already harvesting");
				} else if(msg instanceof MetadataItem) {
					log.debug("item harvested: " + msg);
					
					MetadataItem metadataItem = (MetadataItem)msg;
					String alternateTitle = metadataItem.getAlternateTitle();
					if(alternateTitle != null 
							&& !alternateTitle.trim().isEmpty() 
							&& alternateTitle.contains(" ")) {
						final String tableName = alternateTitle.substring(0, alternateTitle.indexOf(" "));
						
						log.debug("requesting table: " + tableName);						
						database.tell(new FetchTable(tableName), getSelf());
						getContext().become(receivingData(), false);						 
					} else {
						metadata.tell(new NextItem(), getSelf());
					}
				} else if(msg instanceof End) {
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
