package nl.idgis.publisher.service.geoserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.service.manager.messages.Style;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.NextItem;
import static java.util.stream.Collectors.toSet;

public class InfoCollector extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Set<ActorRef> targets;

	private Service service;
	
	private List<Style> styles;
	
	private boolean stylesReceived;
	
	public InfoCollector(Set<ActorRef> targets) {
		this.targets = targets;
	}
	
	public static Props props(Set<ActorRef> targets) {
		return Props.create(InfoCollector.class, targets);
	}
	
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
		
		styles = new ArrayList<>();
		stylesReceived = false;
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {			
			handleReceiveTimeout();
		} else if(msg instanceof Service) {
			log.debug("service received");
			
			service = (Service)msg;
			handleContentReceived();
		} else if(msg instanceof Style) {
			log.debug("style received");
			
			styles.add((Style)msg);
			getSender().tell(new NextItem(), getSelf());
		} else if(msg instanceof End) {
			log.debug("all styles received");
			
			stylesReceived = true;			
			handleContentReceived();
		} else {
			unhandled(msg);
		}
	}
	
	private Procedure<Object> waiting(Set<ActorRef> ensureServices) {
		return new Procedure<Object>() {

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Terminated) {
					ActorRef actor = ((Terminated)msg).getActor();
					if(ensureServices.contains(actor)) {
						ensureServices.remove(actor);
						
						if(ensureServices.isEmpty()) {
							log.debug("all ensure services are terminated");
							
							getContext().stop(getSelf());
						}
					} else {
						log.error("unknown actor terminated: {}", actor);
					}
				} else {
					unhandled(msg);
				}
			}
			
		};
	}
	
	private void handleContentReceived() {
		if(service == null || !stylesReceived) {
			return;
		}
		
		log.debug("starting ensure services");
				
		Set<ActorRef> ensureServices = 
			targets.stream()
				.map(target -> getContext().actorOf(EnsureService.props(target, service, styles)))
				.collect(toSet());
		
		ensureServices.stream().forEach(getContext()::watch);
		getContext().become(waiting(ensureServices));
	}
	
	private void handleReceiveTimeout() {
		log.error("timeout");
		
		getContext().stop(getSelf());
	}
}
