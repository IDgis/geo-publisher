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

import nl.idgis.publisher.service.geoserver.messages.EnsureFeatureTypeLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureGroupLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureWorkspace;
import nl.idgis.publisher.service.geoserver.messages.Ensured;
import nl.idgis.publisher.service.geoserver.messages.FinishEnsure;
import nl.idgis.publisher.service.manager.messages.Layer;
import nl.idgis.publisher.service.manager.messages.Service;

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
									layer.getAbstract()), getSelf());							
							getContext().become(layers(layer.asGroup().getLayers(), depth + 1), false);
						} else {
							getContext().parent().tell(
								new EnsureFeatureTypeLayer(
									layer.getName(), 
									layer.getTitle(), 
									layer.getAbstract(), 
									layer.asDataset().getTableName()), getSelf());
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
		
		getContext().parent().tell(new EnsureWorkspace(service.getId()), getSelf());
		getContext().become(layers(service.getLayers()), false);
	}
	
	private void handleReceiveTimeout() {
		log.error("timeout");
		
		getContext().stop(getSelf());
	}
}
