package nl.idgis.publisher.service.geoserver;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.service.geoserver.messages.EnsureFeatureTypeLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureGroupLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureWorkspace;
import nl.idgis.publisher.service.geoserver.messages.Ensured;
import nl.idgis.publisher.service.geoserver.messages.FinishEnsure;

public class EnsureService extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	public static Props props() {
		return Props.create(EnsureService.class);
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
		} else if(msg instanceof Ensured) {
			log.debug("ensured (root)");
			
			getContext().stop(getSelf());
		} else {
			log.debug("unhandled (root): {}", msg);
			
			unhandled(msg);
		}
	}
	
	private Procedure<Object> layers(List<Layer> layers) {
		return layers(layers, 0);
	}
	
	private Procedure<Object> layers(List<Layer> layers, int depth) {
		log.debug("-> layers {}", depth);
		
		return new Procedure<Object>() {
			
			Iterator<Layer> itr = layers.iterator();

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ensured) {
					log.debug("ensured (layers)");
					
					if(itr.hasNext()) {
						Layer layer = itr.next();
						
						if(layer.isGroup()) {
							getContext().parent().tell(
								new EnsureGroupLayer(
									layer.getName(), 
									layer.getTitle(), 
									layer.getAbstract(),
									layer.getTiling().orElse(null)), getSelf());							
							getContext().become(layers(layer.asGroup().getLayers(), depth + 1), false);
						} else {
							getContext().parent().tell(
								new EnsureFeatureTypeLayer(
									layer.getName(), 
									layer.getTitle(), 
									layer.getAbstract(), 
									layer.asDataset().getTableName(),
									layer.getTiling().orElse(null)), getSelf());
						}
					} else {
						log.debug("unbecome {}", depth);
						
						getContext().parent().tell(new FinishEnsure(), getSelf());
						getContext().unbecome();
					}
				} else {
					log.debug("unhandled (layers): {}", msg);
					
					unhandled(msg);
				}
			}
			
		};
	}
	
	private void handleService(Service service) {
		log.debug("service info received");
		
		getContext().parent().tell(
			new EnsureWorkspace(
				service.getName(), 
				service.getTitle(),
				service.getAbstract(),
				service.getKeywords(),
				service.getContact(),
				service.getOrganization(),
				service.getPosition(),
				service.getAddressType(),
				service.getAddress(),
				service.getCity(),
				service.getState(),
				service.getZipcode(),
				service.getCountry(),
				service.getTelephone(),
				service.getFax(),
				service.getEmail()),
			getSelf());
		getContext().become(layers(service.getLayers()), false);
	}
	
	private void handleReceiveTimeout() {
		log.error("timeout");
		
		getContext().stop(getSelf());
	}
}
