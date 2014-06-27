package nl.idgis.publisher.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.idgis.publisher.monitor.messages.GetStatus;
import nl.idgis.publisher.monitor.messages.NewResource;
import nl.idgis.publisher.monitor.messages.ResourceDestroyed;
import nl.idgis.publisher.monitor.messages.ResourceRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Monitor extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);	
	private final Map<Object, Set<Object>> allResources;
	
	public Monitor() {
		allResources = new HashMap<>();
	}
	
	public static Props props() {
		return Props.create(Monitor.class);
	}
	
	@Override
	public void preStart() {
		log.debug("monitor started");
	}
	
	private Set<Object> getResourcesOfType(ResourceRef resourceRef) {
		Object resourceType = resourceRef.getResourceType();
		
		if(allResources.containsKey(resourceType)) {
			return allResources.get(resourceType);
		} else {
			Set<Object> resources = new HashSet<>();
			allResources.put(resourceType, resources);
			return resources;
		}
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof ResourceRef) {
			Set<Object> resources = getResourcesOfType((ResourceRef) msg);
			
			if(msg instanceof NewResource) {
				resources.add(((ResourceRef) msg).getResource());
			} else if(msg instanceof ResourceDestroyed) {
				resources.remove(((ResourceRef) msg).getResource());
			}
			
		} else if(msg instanceof GetStatus) {
			System.out.println("Resources:");
			System.out.println(allResources);
		} else {
			unhandled(msg);
		}
	}
}
