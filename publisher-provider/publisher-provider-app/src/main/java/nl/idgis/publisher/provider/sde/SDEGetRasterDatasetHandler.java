package nl.idgis.publisher.provider.sde;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.folder.messages.FetchEntireFile;
import nl.idgis.publisher.folder.messages.StopDatasetImport;
import nl.idgis.publisher.metadata.MetadataDocument;
import nl.idgis.publisher.metadata.MetadataDocumentFactory;
import nl.idgis.publisher.provider.protocol.DatasetNotAvailable;
import nl.idgis.publisher.provider.protocol.GetRasterDataset;
import nl.idgis.publisher.utils.FutureUtils;
import scala.concurrent.duration.Duration;

public class SDEGetRasterDatasetHandler extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef originalSender;
	
	private final GetRasterDataset originalMsg;
	
	private final ActorRef rasterFolder;
	
	private ActorRef transaction;

	private FutureUtils f;
	
	public SDEGetRasterDatasetHandler(ActorRef originalSender, GetRasterDataset originalMsg, ActorRef rasterFolder) {
		this.originalSender = originalSender;
		this.originalMsg = originalMsg;
		this.rasterFolder = rasterFolder;
	}
	
	public static Props props(ActorRef originalSender, GetRasterDataset originalMsg, ActorRef rasterFolder) {
		return Props.create(SDEGetRasterDatasetHandler.class, originalSender, originalMsg, rasterFolder);
	}
	
	@Override
	public void preStart() {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
		f = new FutureUtils(getContext());
	}
	
	private void sendUnavailableAndStop() {
		originalSender.tell(new DatasetNotAvailable(originalMsg.getIdentification()), originalSender);
		getContext().stop(getSelf());
	}
	
	private void unavailable() {
		log.error("dataset unavailable");
		
		if(transaction == null) {
			sendUnavailableAndStop();
		} else {
			f.ask(transaction, new Commit()).thenRun(this::sendUnavailableAndStop);
		}
	}
	
	private Procedure<Object> onReceiveItemRecords() {
		return new Procedure<Object>() {
			
			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof SDEItemInfo) {
					log.debug("item info received");
					
					SDEItemInfo itemInfo = (SDEItemInfo)msg;
					SDEItemInfoType type = itemInfo.getType();
					if(SDEItemInfoType.RASTER_DATASET == type) {
						itemInfo.getDocumentation().ifPresent(documentation -> {
							
							try {
								MetadataDocumentFactory mdf = new MetadataDocumentFactory();
								MetadataDocument md = mdf.parseDocument(documentation.getBytes("utf-8"));
								
								Path fileDate = Paths.get(itemInfo.getPhysicalname() + "_date.txt");
								log.debug("fetching fileDate: {}", fileDate);
								Date revisionDate = md.getDatasetRevisionDate();
								
								rasterFolder.tell(new FetchEntireFile(revisionDate, fileDate, 
										itemInfo.getPhysicalname()), originalSender);
								
								getContext().stop(getSelf());
							} catch (Exception e) {
								log.error(e, "couldn't process documentation content");
								
								getContext().stop(getSelf());
							}
						});
					} else {
						log.error("wrong item type: {}", type);
						unavailable();
					}
				} else if(msg instanceof StopDatasetImport) {
					log.debug("stop requested");
					unavailable();
				} else if(msg instanceof ReceiveTimeout) {
					log.debug("timeout received");
					unavailable();
				}
			}
			
		};
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof TransactionCreated) {
			transaction = ((TransactionCreated)msg).getActor();
			ActorRef recordsReceiver = getContext().actorOf(
				SDEReceiveSingleItemInfo.props(getSelf()), 
				"item-records-receiver");
			
			transaction.tell(
				SDEUtils.getFetchTable(SDEUtils.getItemsFilter(originalMsg.getIdentification())),
				recordsReceiver);
			getContext().become(onReceiveItemRecords());
		} else if(msg instanceof StopDatasetImport) {
			log.debug("stop requested");
			unavailable();
		} else if(msg instanceof ReceiveTimeout) {
			log.error("timeout received");
			unavailable();
		} else {
			unhandled(msg);
		}
	}

}
