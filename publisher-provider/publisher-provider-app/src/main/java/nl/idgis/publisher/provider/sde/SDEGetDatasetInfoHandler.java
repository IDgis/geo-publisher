package nl.idgis.publisher.provider.sde;

import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.DatasetInfo;
import nl.idgis.publisher.provider.protocol.DatasetNotFound;
import nl.idgis.publisher.provider.protocol.GetDatasetInfo;

import scala.concurrent.duration.Duration;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

public class SDEGetDatasetInfoHandler extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef originalSender;
	
	private final ActorRef rasterFolder;
	
	private final GetDatasetInfo originalMsg;
	
	private ActorRef transaction;

	private Config databaseConfig;

	private Config rasterConfig;

	private SDEUtils sdeUtils;

	public SDEGetDatasetInfoHandler(ActorRef originalSender, GetDatasetInfo originalMsg, ActorRef rasterFolder, Config databaseConfig, Config rasterConfig) {
		this.originalSender = originalSender;
		this.originalMsg = originalMsg;
		this.rasterFolder = rasterFolder;
		this.databaseConfig = databaseConfig;
		this.rasterConfig = rasterConfig;
		this.sdeUtils = new SDEUtils(databaseConfig);
	}
	
	@Override
	public void preStart() {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
	}
	
	public static Props props(ActorRef originalSender, GetDatasetInfo originalMsg, ActorRef rasterFolder, Config databaseConfig, Config rasterConfig) {
		return Props.create(SDEGetDatasetInfoHandler.class, originalSender, originalMsg, rasterFolder, databaseConfig, rasterConfig);
	}
	
	private Procedure<Object> onReceiveCommitAck() {
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
	
	private void unavailable() {
		log.error("dataset unavailable");
		
		originalSender.tell(new DatasetNotFound(originalMsg.getIdentification()), originalSender);
		
		if(transaction == null) {
			getContext().stop(getSelf());
		} else {
			transaction.tell(new Commit(), getSelf());
			getContext().become(onReceiveCommitAck());
		}
	}
	
	private Procedure<Object> onReceiveDatasetInfo() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof DatasetInfo) {
					log.debug("dataset info received");
					originalSender.tell(msg, getSelf());
					transaction.tell(new Commit(), getSelf());
					getContext().become(onReceiveCommitAck());
				} else if(msg instanceof ReceiveTimeout) {
					unavailable();
				} else {
					unhandled(msg);
				}
			}
		};
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof TransactionCreated) {
			log.debug("transaction created");
			
			transaction = ((TransactionCreated)msg).getActor();
			
			ActorRef datasetInfoGatherer = getContext().actorOf(
					SDEGatherDatasetInfo.props(
						getSelf(), 
						transaction, 
						rasterFolder, 
						originalMsg.getAttachmentTypes(),
						databaseConfig,
						rasterConfig),
					"dataset-info-gatherer");
			
			ActorRef itemInfoReceiver = getContext().actorOf(
				SDEReceiveSingleItemInfo.props(datasetInfoGatherer), 
				"item-info-receiver");

			String databaseScheme;
			try {
				databaseScheme = databaseConfig.getString("scheme");
			} catch(ConfigException.Missing cem) {
				databaseScheme = "SDE";
			}

			log.debug("database scheme before calling get fetch table: " + databaseScheme);

			transaction.tell(
				sdeUtils.getFetchTable(sdeUtils.getItemsFilter(originalMsg.getIdentification()), databaseScheme),
				itemInfoReceiver);
			
			getContext().become(onReceiveDatasetInfo());
		} else if(msg instanceof ReceiveTimeout){
			log.error("timeout received");
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
}
