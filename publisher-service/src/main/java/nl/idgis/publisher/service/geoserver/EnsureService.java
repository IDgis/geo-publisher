package nl.idgis.publisher.service.geoserver;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Procedure;

import scala.concurrent.duration.Duration;

import nl.idgis.publisher.domain.web.tree.DatasetLayer;
import nl.idgis.publisher.domain.web.tree.DatasetLayerRef;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Service;
import nl.idgis.publisher.domain.web.tree.VectorDatasetLayer;

import nl.idgis.publisher.service.geoserver.messages.EnsureFeatureTypeLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureGroupLayer;
import nl.idgis.publisher.service.geoserver.messages.EnsureStyle;
import nl.idgis.publisher.service.geoserver.messages.EnsureWorkspace;
import nl.idgis.publisher.service.geoserver.messages.Ensured;
import nl.idgis.publisher.service.geoserver.messages.FinishEnsure;
import nl.idgis.publisher.service.manager.messages.Style;

public class EnsureService extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef target;
	
	private final Service service;
	
	private final List<Style> styles;
	
	public EnsureService(ActorRef target, Service service, List<Style> styles) {
		this.target = target;
		this.service = service;
		this.styles = styles;
	}
	
	private Set<String> layerNames;
	
	public static Props props(ActorRef target, Service service, List<Style> styles) {
		return Props.create(EnsureService.class, target, service, styles);
	}
	
	public void preStart() throws Exception {
		getContext().setReceiveTimeout(Duration.create(30, TimeUnit.SECONDS));
		
		layerNames = new HashSet<>();
		processStyles();
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ReceiveTimeout) {			
			handleReceiveTimeout();
		} else if(msg instanceof Ensured) {
			log.debug("ensured (root)");
			
			getContext().stop(getSelf());
		} else {
			log.debug("unhandled (root): {}", msg);
			
			unhandled(msg);
		}
	}

	private Procedure<Object> layers(List<LayerRef<?>> layers) {
		return layers(layers, 0);
	}
	
	private Procedure<Object> layers(List<LayerRef<?>> layers, int depth) {
		log.debug("-> layers {}", depth);
		
		return new Procedure<Object>() {
			
			Iterator<LayerRef<?>> itr = layers.iterator();

			@Override
			public void apply(Object msg) throws Exception {
				if(msg instanceof Ensured) {
					log.debug("ensured (layers)");
					
					if(itr.hasNext()) {
						LayerRef<?> layerRef = itr.next();
						
						if(layerRef.isGroupRef()) {
							GroupLayer layer = layerRef.asGroupRef().getLayer();
							
							target.tell(
								new EnsureGroupLayer(
									getUniqueLayerName(layer.getName()), 
									layer.getTitle(), 
									layer.getAbstract(),
									layer.getTiling().orElse(null)), getSelf());							
							getContext().become(layers(layer.getLayers(), depth + 1), false);
						} else {
							DatasetLayerRef datasetRef = layerRef.asDatasetRef();
							DatasetLayer layer = datasetRef.getLayer();
							
							String defaultStyleName;
							List<String> additionalStyleNames;
							
							List<String> styleNames = layer.getStyleRefs().stream()
								.map(styleRef -> styleRef.getName())
								.collect(Collectors.toList());
							
							if(styleNames.isEmpty()) {
								defaultStyleName = null;
								additionalStyleNames = Collections.emptyList();
							} else {
								defaultStyleName = styleNames.get(0);								
								additionalStyleNames = styleNames.stream()
									.skip(1)
									.collect(Collectors.toList());
							}
							
							if(layer.isVectorLayer()) {
								VectorDatasetLayer vectorLayer = layer.asVectorLayer();
								
								target.tell(
									new EnsureFeatureTypeLayer(
										getUniqueLayerName(layer.getName()), 
										layer.getTitle(), 
										layer.getAbstract(), 
										layer.getKeywords(),
										vectorLayer.getTableName(),
										vectorLayer.getColumnNames(),
										layer.getTiling().orElse(null),
										defaultStyleName,
										datasetRef.getStyleRef() == null 
											? null
											: datasetRef.getStyleRef().getName(),
										additionalStyleNames), getSelf());
							} else { // TODO: add support for raster layers
								log.error("unsupported layer type");
								getSelf().tell(new Ensured(), getSelf());
							}
						}
					} else {
						log.debug("unbecome {}", depth);
						
						target.tell(new FinishEnsure(), getSelf());
						getContext().unbecome();
					}
				} else {
					log.debug("unhandled (layers): {}", msg);
					
					unhandled(msg);
				}
			}
			
		};
	}
	
	private Procedure<Object> styles(List<Style> styles) {
		return new Procedure<Object>() {
			
			Iterator<Style> itr = styles.iterator();
			
			public void apply(Object msg) {
				if(msg instanceof Ensured) {
					log.debug("ensured (styles)");
					
					if(itr.hasNext()) {
						Style style = itr.next();
						target.tell(new EnsureStyle(style.getStyleName(), style.getSld()), getSelf());
					} else {
						getContext().become(receive());
						processService();
					}
				} else {
					log.debug("unhandled (styles): {}", msg);
					
					unhandled(msg);
				}
			}
		};
	}

	private void processStyles() {
		getSelf().tell(new Ensured(), getSelf());
		getContext().become(styles(styles));
	}

	private void processService() {
		target.tell(
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
	
	private String getUniqueLayerName(String layerName) {
		int layerNamePostfixCount = 2;
		
		String uniqueLayerName = layerName;
		while(layerNames.contains(uniqueLayerName)) {
			uniqueLayerName = layerName + "-" + layerNamePostfixCount++;
		}
		
		layerNames.add(uniqueLayerName);
		
		return uniqueLayerName;
	}
}
