package nl.idgis.publisher.harvester;

import nl.idgis.publisher.harvester.messages.Harvest;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.protocol.database.DescribeTable;
import nl.idgis.publisher.provider.protocol.database.TableDescription;
import nl.idgis.publisher.provider.protocol.metadata.GetMetadata;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
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
			
			getSelf().tell(new Harvest(), getSelf());
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
	
	private Procedure<Object> requestingTableDescription(final ActorRef metadataCursor) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof TableDescription) {
					log.debug(msg.toString());
					finish();
				} else if(msg instanceof Failure) {
					log.error(((Failure) msg).getCause(), "data retrieval failure");
					finish();
				} else {
					unhandled(msg);
				}
			}	
			
			void finish() {
				metadataCursor.tell(new NextItem(), getSelf());					
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
						
						log.debug("requesting table structure: " + tableName);						
						database.tell(new DescribeTable(tableName), getSelf());
						getContext().become(requestingTableDescription(getSender()), false);						 
					} else {
						getSender().tell(new NextItem(), getSelf());
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
