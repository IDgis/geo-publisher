package nl.idgis.publisher.harvester.sources;

import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.harvester.sources.messages.GetDatasets;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.protocol.database.FetchTable;
import nl.idgis.publisher.provider.protocol.metadata.GetAllMetadata;
import nl.idgis.publisher.provider.protocol.metadata.GetMetadata;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.utils.Ask;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp.ConnectionClosed;
import akka.japi.Procedure;

public class ProviderClient extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final String harvesterName;
	private final ActorRef harvester, metadata, database;
		
	public ProviderClient(String harvesterName, ActorRef harvester, ActorRef metadata, ActorRef database) {
		this.harvesterName = harvesterName;
		this.harvester = harvester;
		this.metadata = metadata;
		this.database = database;
	}
	
	public static Props props(String harvesterName, ActorRef harvester, ActorRef metadata, ActorRef database) {
		return Props.create(ProviderClient.class, harvesterName, harvester, metadata, database);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof Hello) {
			log.debug(msg.toString());
			
			getSender().tell(new Hello(harvesterName), getSelf());
			getContext().become(active(), false);
			harvester.tell(new DataSourceConnected(((Hello) msg).getName()), getSelf());
		} else if(msg instanceof ConnectionClosed) {
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
				if(msg instanceof GetDatasets) {
					log.debug("retrieving datasets from provider");
					
					ActorRef providerDataset = getContext().actorOf(ProviderDatasetInfo.props(getSender(), database));
					metadata.tell(new GetAllMetadata(), providerDataset);
				} else if(msg instanceof ConnectionClosed) {
					log.debug("disconnected");
					getContext().stop(getSelf());
				} else if(msg instanceof GetDataset) {
					log.debug("retrieving data from provider");
					
					final GetDataset gd = (GetDataset)msg;					
					Ask.ask(getContext(), metadata, new GetMetadata(gd.getId()), 15000)
						.onSuccess(new OnSuccess<Object>() {

							@Override
							public void onSuccess(Object msg) throws Throwable { 
								MetadataItem metadataItem = (MetadataItem)msg;
								
								log.debug("metadata retrieved");
								String tableName = ProviderUtils.getTableName(metadataItem);
								if(tableName == null) {
									log.warning("no table name for dataset");
								} else {
									log.debug("requesting table");
									
									ActorRef receiver = getContext().actorOf(gd.getReceiverProps());									
									database.tell(new FetchTable(tableName, gd.getColumns()), receiver); 
								}
							}
						}, getContext().dispatcher());
				} else {
					unhandled(msg);
				} 
			}
		};
	}
}
