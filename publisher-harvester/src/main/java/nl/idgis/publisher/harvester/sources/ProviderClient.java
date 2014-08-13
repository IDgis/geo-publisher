package nl.idgis.publisher.harvester.sources;

import nl.idgis.publisher.harvester.messages.DataSourceConnected;
import nl.idgis.publisher.harvester.metadata.messages.GetAlternateTitle;
import nl.idgis.publisher.harvester.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.harvester.sources.messages.GetDatasets;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.protocol.messages.Hello;
import nl.idgis.publisher.provider.protocol.database.FetchTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
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
import akka.pattern.Patterns;

public class ProviderClient extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final int FETCH_TABLE_MESSAGE_SIZE = 10;
	
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
	
	private void processMetadata(final GetDataset gd, String alternateTitle) {
		log.debug("processing metadata");
		
		final String tableName = ProviderUtils.getTableName(alternateTitle);
		if(tableName == null) {
			log.warning("no table name for dataset");
		} else {
			log.debug("requesting table count");
			
			Ask.ask(getContext(), database, new PerformCount(tableName), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						Long count = (Long)msg;
						
						log.debug("count: " + count + ", starting import");
						
						final ActorRef receiver = getContext().actorOf(gd.getReceiverProps());
						Patterns.ask(receiver, new StartImport(count), 15000)
							.onSuccess(new OnSuccess<Object>() {

								@Override
								public void onSuccess(Object msg) throws Throwable {
									log.debug("requesting table");
									
									database.tell(new FetchTable(tableName, gd.getColumns(), FETCH_TABLE_MESSAGE_SIZE), receiver);
								}
								
							}, getContext().dispatcher());												
					}
				}, getContext().dispatcher()); 
		}
	}
	
	private Procedure<Object> active() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof GetDatasets) {
					log.debug("retrieving datasets from provider");
					
					ActorRef providerDataset = getContext().actorOf(ProviderDatasetInfo.props(getSender(), harvester, database));
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
								Patterns.ask(harvester, new ParseMetadataDocument(metadataItem.getContent()), 15000)
									.onSuccess(new OnSuccess<Object>() {

										@Override
										public void onSuccess(Object o) throws Throwable {
											ActorRef metadataDocument = (ActorRef)o;
											
											Patterns.ask(metadataDocument, new GetAlternateTitle(), 15000)
												.onSuccess(new OnSuccess<Object>() {

													@Override
													public void onSuccess(Object o) throws Throwable {
														String alternateTitle = (String)o;
														
														log.debug("metadata parsed");
														
														processMetadata(gd, alternateTitle);
													}
													
												}, getContext().dispatcher());
										}
										
									}, getContext().dispatcher());
							}
						}, getContext().dispatcher());
				} else {
					unhandled(msg);
				} 
			}
		};
	}
}
