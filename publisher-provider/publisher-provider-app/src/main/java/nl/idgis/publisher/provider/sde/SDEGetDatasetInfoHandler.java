package nl.idgis.publisher.provider.sde;

import java.util.concurrent.TimeUnit;

import nl.idgis.publisher.database.messages.Commit;
import nl.idgis.publisher.database.messages.TransactionCreated;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.database.messages.DescribeTable;
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
	
	private final GetDatasetInfo originalMsg;
	
	private ActorRef transaction;
		
	public SDEGetDatasetInfoHandler(ActorRef originalSender, GetDatasetInfo originalMsg) {
		this.originalSender = originalSender;
		this.originalMsg = originalMsg;
	}
	
	@Override
	public void preStart() {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
	}
	
	public static Props props(ActorRef originalSender, GetDatasetInfo originalMsg) {
		return Props.create(SDEGetDatasetInfoHandler.class, originalSender, originalMsg);
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
	
	private Procedure<Object> onReceiveItemInfo() {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof SDEItemInfo) {
					log.debug("item info received");
					
					SDEItemInfo itemInfo = (SDEItemInfo)msg;
					String tableName = itemInfo.getPhysicalname();
						
					log.debug("tableName: {}", tableName);
					
					ActorRef tableInfoReceiver = getContext().actorOf(
						SDEReceiveTableInfo.props(
							getSelf(), 
							originalMsg.getIdentification(), 
							tableName),
						"table-info-receiver");
					
					transaction.tell(new DescribeTable(tableName), tableInfoReceiver);
					getContext().become(onReceiveDatasetInfo());
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
			log.debug("transaction created");
			
			transaction = ((TransactionCreated)msg).getActor();
			ActorRef itemInfoReceiver = getContext().actorOf(
				SDEReceiveSingleItemInfo.props(getSelf()), 
				"item-info-receiver");
			
			transaction.tell(
				SDEUtils.getFetchTable(SDEUtils.getItemsFilter(originalMsg.getIdentification())), 
				itemInfoReceiver);
			
			getContext().become(onReceiveItemInfo());
		} else if(msg instanceof ReceiveTimeout){
			log.error("timeout received");
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
	}
}
