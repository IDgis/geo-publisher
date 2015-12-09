package nl.idgis.publisher.service.geoserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.AllForOneStrategy;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.actor.SupervisorStrategy.Directive;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.service.geoserver.messages.EnsureTarget;
import nl.idgis.publisher.service.geoserver.messages.PreviousEnsureInfo;
import nl.idgis.publisher.service.manager.messages.Style;
import nl.idgis.publisher.stream.messages.End;
import nl.idgis.publisher.stream.messages.Item;
import nl.idgis.publisher.stream.messages.NextItem;
import static java.util.stream.Collectors.toSet;

public class InfoCollector extends UntypedActor {
	
	private final static SupervisorStrategy supervisorStrategy = new AllForOneStrategy(10, Duration.create("1 minute"), 
		new Function<Throwable, Directive>() {

		@Override
		public Directive apply(Throwable t) throws Exception {			
			return AllForOneStrategy.escalate();
		}
		
	});
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final Set<EnsureTarget> targets;

	private Service service;
	
	private List<Style> styles;
	
	private boolean stylesReceived;
	
	private PreviousEnsureInfo previousEnsureInfo;
	
	public InfoCollector(Set<EnsureTarget> targets) {
		this.targets = targets;
	}
	
	public static Props props(Set<EnsureTarget> targets) {
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
		} else if(msg instanceof Item) {
			log.debug("item received");
			
			Object item = ((Item<?>)msg).getContent();
			if(item instanceof Style) {
				log.debug("style received");
				
				styles.add((Style)item);
			} else {
				log.error("unknown item received: {}", item);
			}
			
			getSender().tell(new NextItem(), getSelf());
		} else if(msg instanceof End) {
			log.debug("all styles received");
			
			stylesReceived = true;			
			handleContentReceived();
		} else if(msg instanceof PreviousEnsureInfo) {
			log.debug("info about previous ensure received: {}", msg);
			
			previousEnsureInfo = (PreviousEnsureInfo)msg;
			handleContentReceived();
		} else {
			log.debug("unhandled message received: {}", msg);
			
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
		if(service == null || !stylesReceived || previousEnsureInfo == null) {
			return;
		}
		
		log.debug("starting ensure services");
				
		Set<ActorRef> ensureServices = 
			targets.stream()
				.map(target -> {
					ActorRef actorRef = target.getActorRef();
					String metadataInfoLink = target.getEnvironmentInfo()
						.map(environmentId -> environmentId.getMetadataUrl()) // TODO: compose metadata info link
						.orElse(null);
					
					log.debug ("metadata info link: " + metadataInfoLink);
					
					return getContext().actorOf(
						EnsureService.props(actorRef, service, styles, metadataInfoLink, previousEnsureInfo));
				})
				.collect(toSet());
		
		ensureServices.stream().forEach(getContext()::watch);
		getContext().become(waiting(ensureServices));
	}
	
	private void handleReceiveTimeout() {
		log.error("timeout");
		
		getContext().stop(getSelf());
	}
	
	@Override
	public SupervisorStrategy supervisorStrategy() {
		return supervisorStrategy;
	}
}
