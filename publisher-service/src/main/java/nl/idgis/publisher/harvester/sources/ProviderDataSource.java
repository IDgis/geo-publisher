package nl.idgis.publisher.harvester.sources;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.pattern.Patterns;

import nl.idgis.publisher.harvester.sources.messages.GetDataset;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;
import nl.idgis.publisher.harvester.sources.messages.ListDatasets;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.messages.ParseMetadataDocument;
import nl.idgis.publisher.provider.protocol.database.FetchTable;
import nl.idgis.publisher.provider.protocol.database.PerformCount;
import nl.idgis.publisher.provider.protocol.metadata.GetAllMetadata;
import nl.idgis.publisher.provider.protocol.metadata.GetMetadata;
import nl.idgis.publisher.provider.protocol.metadata.MetadataItem;
import nl.idgis.publisher.utils.Ask;

public class ProviderDataSource extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final int FETCH_TABLE_MESSAGE_SIZE = 10;
		
	private final ActorRef harvester, provider;
	
	public ProviderDataSource(ActorRef harvester, ActorRef provider) {
		this.harvester = harvester;
		this.provider = provider;		
	}
	
	public static Props props(ActorRef harvester, ActorRef provider) {
		return Props.create(ProviderDataSource.class, harvester, provider);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ListDatasets) {
			handleListDatasets();
		} else if(msg instanceof GetDatasetMetadata) {
			handleGetDatasetMetadata((GetDatasetMetadata)msg);
		} else if(msg instanceof GetDataset) {
			handleGetDataset((GetDataset)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private void handleListDatasets() {
		log.debug("retrieving datasets from provider");
		
		ActorRef providerDataset = getContext().actorOf(ProviderDatasetInfo.props(getSender(), harvester, provider));
		provider.tell(new GetAllMetadata(), providerDataset);
	}
	
	private void handleGetDataset(final GetDataset gd) {
		log.debug("retrieving data from provider");
		
		final ActorRef initiator = getSender();
		
		Patterns.ask(getSelf(), new GetDatasetMetadata(gd.getId()), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable {
					MetadataDocument metadataDocument = (MetadataDocument)msg;
					
					String alternateTitle = metadataDocument.getAlternateTitle();
					log.debug("alternate title read from metadata");
					
					processMetadata(gd, alternateTitle, initiator);
				}
				
			}, getContext().dispatcher());
	}	
	
	private void processMetadata(final GetDataset gd, String alternateTitle, final ActorRef initiator) {
		log.debug("processing metadata");
		
		final String tableName = ProviderUtils.getTableName(alternateTitle);
		if(tableName == null) {
			log.warning("no table name for dataset");
		} else {
			log.debug("requesting table count");
			
			Ask.ask(getContext(), provider, new PerformCount(tableName), 15000)
				.onSuccess(new OnSuccess<Object>() {

					@Override
					public void onSuccess(Object msg) throws Throwable {
						Long count = (Long)msg;
						
						log.debug("count: " + count + ", starting import");
						
						final ActorRef receiver = getContext().actorOf(gd.getReceiverProps());
						Patterns.ask(receiver, new StartImport(initiator, count), 15000)
							.onSuccess(new OnSuccess<Object>() {

								@Override
								public void onSuccess(Object msg) throws Throwable {
									log.debug("requesting table");
									
									provider.tell(new FetchTable(tableName, gd.getColumns(), FETCH_TABLE_MESSAGE_SIZE), receiver);
								}
								
							}, getContext().dispatcher());												
					}
				}, getContext().dispatcher()); 
		}
	}
	
	private void handleGetDatasetMetadata(GetDatasetMetadata gdm) {				
		log.debug("retrieving dataset metadata from provider");
		
		final ActorRef sender = getSender();
						
		Ask.ask(getContext(), provider, new GetMetadata(gdm.getDatasetId()), 15000)
			.onSuccess(new OnSuccess<Object>() {

				@Override
				public void onSuccess(Object msg) throws Throwable { 
					MetadataItem metadataItem = (MetadataItem)msg;
					
					log.debug("metadata retrieved");								
					Patterns.ask(harvester, new ParseMetadataDocument(metadataItem.getContent()), 15000)
						.onSuccess(new OnSuccess<Object>() {

							@Override
							public void onSuccess(Object o) throws Throwable {
								MetadataDocument metadataDocument = (MetadataDocument)o;
								log.debug("metadata parsed");
								
								sender.tell(metadataDocument, getSelf());
							}
						}, getContext().dispatcher());
				}
			}, getContext().dispatcher());	
	}

}
