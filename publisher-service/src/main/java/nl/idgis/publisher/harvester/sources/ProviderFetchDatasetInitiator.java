package nl.idgis.publisher.harvester.sources;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.harvester.sources.messages.FetchDataset;
import nl.idgis.publisher.harvester.sources.messages.StartImport;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.provider.protocol.AbstractGetDatasetRequest;
import nl.idgis.publisher.provider.protocol.DatasetInfo;

public abstract class ProviderFetchDatasetInitiator<T extends FetchDataset, U extends DatasetInfo> extends UntypedActor {

	protected final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Duration timeout = Duration.create(15, TimeUnit.SECONDS);
	
	protected final ActorRef sender, receiver, provider;
	
	protected final T request;
	
	public ProviderFetchDatasetInitiator(ActorRef sender, T request, ActorRef receiver, ActorRef provider) {
		this.sender = sender;
		this.request = request;
		this.receiver = receiver;
		this.provider = provider;
	}
	
	@Override
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(timeout);
	}
	
	protected abstract StartImport startImport(U datasetInfo);
	
	protected abstract AbstractGetDatasetRequest getDataset();
	
	@Override
	@SuppressWarnings("unchecked")
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			log.error("timeout");
			
			getContext().stop(getSelf());
		} else if(msg instanceof DatasetInfo) {
			log.debug("dataset info received");
			
			receiver.tell(startImport((U)msg), getSelf());
		} else if(msg instanceof Ack) {
			log.debug("receiver is ready");
			
			provider.tell(getDataset(), receiver);
			
			getContext().stop(getSelf());
		} else {
			unhandled(msg);
		}
		
	}
}
