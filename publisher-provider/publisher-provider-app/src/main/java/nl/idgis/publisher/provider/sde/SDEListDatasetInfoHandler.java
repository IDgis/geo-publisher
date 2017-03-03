package nl.idgis.publisher.provider.sde;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
	
	private final boolean includeConfidential;
	
	private ActorRef transaction;
	
	public SDEListDatasetInfoHandler(ActorRef originalSender, ListDatasetInfo originalMsg, ActorRef rasterFolder, boolean includeConfidential) {
		this.originalSender = originalSender;
		this.originalMsg = originalMsg;
		this.rasterFolder = rasterFolder;
		this.includeConfidential = includeConfidential;
	}
	
	public static Props props(ActorRef originalSender, ListDatasetInfo originalMsg, ActorRef rasterFolder, boolean includeConfidential) {
		return Props.create(SDEListDatasetInfoHandler.class, originalSender, originalMsg, rasterFolder, includeConfidential);
	}
	
	@Override
	public void preStart() {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
	}
	
	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof TransactionCreated) {
			log.debug("transaction created");
			
			transaction = ((TransactionCreated)msg).getActor();
			transaction.tell(SDEUtils.getFetchTable(SDEUtils.getItemsFilter()), getSelf());
			
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
			
			ActorRef consumer = originalSender, producer;
			
			long sequenceNumber = 0, filtered = 0; 
			
			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof DatasetInfo) {
					log.debug("dataset info received");
					
					DatasetInfo datasetInfo = (DatasetInfo)msg;
					if(!includeConfidential && datasetInfo.isConfidential()) {
						filtered++;
						producer.tell(new NextItem(), getSelf());
					} else {
						consumer.tell(
							new Item<DatasetInfo>(
								sequenceNumber++,
								datasetInfo), 
							getSelf());
					}
				} else if(msg instanceof Item) {
					log.debug("item received");
					
					Item<?> item = (Item<?>)msg;
					Object content = item.getContent();
					if(content instanceof Records) {
						producer = getSender();
						
						SDEItemInfo itemInfo = SDEUtils.toItemInfo((Records)content);
						
						ActorRef datasetInfoGatherer = getContext().actorOf(
								SDEGatherDatasetInfo.props(
									getSelf(), 
									transaction, 
									rasterFolder, 
									originalMsg.getAttachmentTypes()),
								nameGenerator.getName(SDEGatherDatasetInfo.class));
						
						datasetInfoGatherer.tell(itemInfo, getSelf());
					} else {
						unhandled(msg);
					}
				} else if(msg instanceof NextItem) {
					log.debug("next item");
					
					NextItem nextItem;
					Optional<Long> nextSequenceNumber = ((NextItem)msg).getSequenceNumber();
					if(nextSequenceNumber.isPresent()) {
						nextItem = new NextItem(nextSequenceNumber.get() + filtered);
					} else {
						nextItem = new NextItem();
					}
					
					producer.tell(nextItem, getSelf());
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
