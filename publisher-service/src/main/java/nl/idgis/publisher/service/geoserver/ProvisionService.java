package nl.idgis.publisher.service.geoserver;

import java.util.concurrent.TimeUnit;

import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.web.Service;

public class ProvisionService extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	public static Props props() {
		return Props.create(ProvisionService.class);
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
		
		getContext().stop(getSelf());
	}
	
	private void handleReceiveTimeout() {
		log.error("timeout");
		
		getContext().stop(getSelf());
	}
}
