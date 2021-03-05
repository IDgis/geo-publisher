package nl.idgis.publisher.provider.sde;

import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.ListDatasetInfo;
import nl.idgis.publisher.provider.protocol.Records;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import nl.idgis.publisher.stream.messages.Stop;
import nl.idgis.publisher.stream.messages.Unavailable;
import nl.idgis.publisher.utils.UniqueNameGenerator;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class SDEListDatasetInfoHandler extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final UniqueNameGenerator nameGenerator = new UniqueNameGenerator();
	
	private final ActorRef originalSender;

	private final ListDatasetInfo originalMsg;
	
	private final ActorRef rasterFolder;
	
	private ActorRef transaction;
	
	private String databaseScheme;

	private String databaseVendor;
	
	private Config databaseConfig;
	
	private Config rasterConfig;
	
	public SDEListDatasetInfoHandler(ActorRef originalSender, ListDatasetInfo originalMsg, ActorRef rasterFolder, Config databaseConfig, Config rasterConfig) {
		this.originalSender = originalSender;
		this.originalMsg = originalMsg;
		this.rasterFolder = rasterFolder;
		this.databaseConfig = databaseConfig;
		this.rasterConfig = rasterConfig;
	}
	
	public static Props props(ActorRef originalSender, ListDatasetInfo originalMsg, ActorRef rasterFolder, Config databaseConfig, Config rasterConfig) {
		return Props.create(SDEListDatasetInfoHandler.class, originalSender, originalMsg, rasterFolder, databaseConfig, rasterConfig);
	}
	
	@Override
	public void preStart() {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof TransactionCreated) {
			log.debug("transaction created");
			
			try {
				databaseScheme = databaseConfig.getString("scheme");
			} catch(ConfigException.Missing cem) {
				databaseScheme = "SDE";
			}
			
			log.debug("database scheme before calling get fetch table: " + databaseScheme);

			databaseVendor = databaseConfig.getString("vendor");
			log.debug("database vendor before calling get fetch table: " + databaseVendor);
			
			transaction = ((TransactionCreated)msg).getActor();
			transaction.tell(SDEUtils.getFetchTable(SDEUtils.getItemsFilter(databaseVendor), databaseScheme, databaseVendor), getSelf());
			
			getContext().become(onReceiveStreaming());
		} else if(msg instanceof ReceiveTimeout) {
			log.error("timeout received");
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
	
	protected void terminate() {
		transaction.tell(new Commit(), getSelf());
		getContext().become(onReceiveCommitAck());
	}
	
	protected Procedure<Object> onReceiveCommitAck() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ack) {
					log.debug("transaction finished");
					getContext().stop(getSelf());
				} else if(msg instanceof ReceiveTimeout) {
					log.error("timeout received");
					getContext().stop(getSelf());
				}
			}
			
		};
	}
	
	private Procedure<Object> onReceiveStreaming() {
		return new Procedure<Object>() {
			
			Item<?> item;
			
			ActorRef consumer = originalSender, producer;

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof DatasetInfo) {
					log.debug("dataset info received");
					consumer.tell(
						new Item<DatasetInfo>(
							item.getSequenceNumber(), 
							(DatasetInfo)msg), 
						getSelf());
				} else if(msg instanceof Item) {
					log.debug("item received");
					
					item = (Item<?>)msg;
					Object content = item.getContent();
					if(content instanceof Records) {
						producer = getSender();
						
						SDEItemInfo itemInfo = SDEUtils.toItemInfo((Records)content);
						
						ActorRef datasetInfoGatherer = getContext().actorOf(
								SDEGatherDatasetInfo.props(
									getSelf(), 
									transaction, 
									rasterFolder, 
									originalMsg.getAttachmentTypes(),
									databaseConfig,
									rasterConfig),
								nameGenerator.getName(SDEGatherDatasetInfo.class));
						
						datasetInfoGatherer.tell(itemInfo, getSelf());
					} else {
						unhandled(msg);
					}
				} else if(msg instanceof NextItem) {
					log.debug("next item");
					producer.tell(msg, getSelf());
				} else if(msg instanceof End) {
					log.debug("end");
					consumer.tell(msg, getSelf());
					terminate();
				} else if(msg instanceof Stop) {
					log.debug("stop");
					terminate();
				} else if(msg instanceof Unavailable) {
					log.debug("unavailable");
					terminate();
					consumer.tell(msg, getSelf());
				} else if(msg instanceof ReceiveTimeout) {
					log.error("timeout received");
					consumer.tell(new Unavailable(), getSelf());
					terminate();
				} else {
					unhandled(msg);
				}
			}
			
		};
	}	

}
