package nl.idgis.publisher.harvester.sources;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.harvester.sources.messages.FetchVectorDataset;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.GetVectorDataset;
import nl.idgis.publisher.provider.protocol.VectorDatasetInfo;

public class ProviderGetDatasetInitiater extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final int GET_VECTOR_DATASET_MESSAGE_SIZE = 10;
	
	private final Duration timeout = Duration.create(15, TimeUnit.SECONDS);
	
	private final FetchVectorDataset request;
	
	private final ActorRef sender, receiver, provider;
	
	public ProviderGetDatasetInitiater(ActorRef sender, FetchVectorDataset request, ActorRef receiver, ActorRef provider) {
		this.sender = sender;		
		this.receiver = receiver;
		this.provider = provider;
		
		this.request = request;
	}
	
	public static Props props(ActorRef sender, FetchVectorDataset request, ActorRef receiver, ActorRef provider) {
		return Props.create(ProviderGetDatasetInitiater.class, sender, request, receiver, provider);
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(timeout);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout");
			
			getContext().stop(getSelf());
		} else if(msg instanceof VectorDatasetInfo) {
			log.debug("vector dataset info received");
			
			receiver.tell(new StartImport(sender, ((VectorDatasetInfo)msg).getNumberOfRecords()), getSelf());
		} else if(msg instanceof Ack) {
			log.debug("receiver is ready");
			
			provider.tell(new GetVectorDataset(request.getId(), request.getColumns(), GET_VECTOR_DATASET_MESSAGE_SIZE), receiver);
			
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
		
	}

}
