package nl.idgis.publisher.service.geoserver;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.web.Service;

import nl.idgis.publisher.protocol.messages.Ack;

public class ServiceProvisioning extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef initiator;
	
	public ServiceProvisioning(ActorRef initiator) {
		this.initiator = initiator;
	}
	
	public static Props props(ActorRef initiator) {
		return Props.create(ServiceProvisioning.class, initiator);
	}
	
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {
			handleReceiveTimeout();
		} else if(msg instanceof Service) {
			handleService((Service)msg);
		} else {
			unhandled(msg);
		}
	}
	
	private void handleService(Service service) {
		log.debug("service info received");
		
		// TODO: actually do something
		
		initiator.tell(new Ack(), getSelf());
	}
	
	private void handleReceiveTimeout() {
		log.error("timeout");
		
		getContext().stop(getSelf());
	}
}
