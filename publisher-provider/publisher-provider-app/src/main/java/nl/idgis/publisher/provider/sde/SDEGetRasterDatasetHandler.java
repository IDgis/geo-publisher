package nl.idgis.publisher.provider.sde;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;
import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.folder.messages.FetchFile;
import nl.idgis.publisher.provider.database.DatabaseType;
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

	private Config databaseConfig;

	private SDEUtils sdeUtils;
	
	public SDEGetRasterDatasetHandler(ActorRef originalSender, GetRasterDataset originalMsg, ActorRef rasterFolder, Config databaseConfig) {
		this.originalSender = originalSender;
		this.originalMsg = originalMsg;
		this.rasterFolder = rasterFolder;
		this.databaseConfig = databaseConfig;
		this.sdeUtils = new SDEUtils(databaseConfig);
	}
	
	public static Props props(ActorRef originalSender, GetRasterDataset originalMsg, ActorRef rasterFolder, Config databaseConfig) {
		return Props.create(SDEGetRasterDatasetHandler.class, originalSender, originalMsg, rasterFolder, databaseConfig);
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

						DatabaseType databaseVendor;
						if (databaseConfig.hasPath("vendor")) {
							try {
								databaseVendor = DatabaseType.valueOf(databaseConfig.getString("vendor").toUpperCase());
							} catch(IllegalArgumentException iae) {
								throw new ConfigException.BadValue("vendor", "Invalid vendor supplied in config");
							}
						} else {
							databaseVendor = DatabaseType.ORACLE;
						}

						String physicalname = itemInfo.getPhysicalname();
						String filename = DatabaseType.POSTGRES == databaseVendor ? physicalname.toLowerCase() : physicalname;

						Path file = Paths.get(filename + ".tif");
						
						log.debug("fetching file: {}", file);
						rasterFolder.tell(new FetchFile(file), originalSender);
						getContext().stop(getSelf());
					} else {
						log.error("wrong item type: {}", type);
						unavailable();
					}
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

			String databaseScheme;
			try {
				databaseScheme = databaseConfig.getString("scheme");
			} catch(ConfigException.Missing cem) {
				databaseScheme = "SDE";
			}
			
			log.debug("database scheme before calling get fetch table: " + databaseScheme);

			String databaseVendor = databaseConfig.getString("vendor");
			log.debug("database vendor before calling get fetch table: " + databaseVendor);
			
			transaction.tell(
				sdeUtils.getFetchTable(sdeUtils.getItemsFilter(originalMsg.getIdentification()), databaseScheme),
				recordsReceiver);
			getContext().become(onReceiveItemRecords());
		} else if(msg instanceof ReceiveTimeout) {
			log.error("timeout received");
			unavailable();
		} else {
			unhandled(msg);
		}
	}

}
