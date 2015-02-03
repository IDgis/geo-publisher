package nl.idgis.publisher.monitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.idgis.publisher.monitor.messages.GetResources;
import nl.idgis.publisher.monitor.messages.GetTree;
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
		
		getContext().actorOf(Actors.props(getSelf()), "actors");
	}
	
	private Set<Object> getResourcesOfType(Object resourceType) {
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
			log.debug(msg.toString());
			
			Set<Object> resources = getResourcesOfType(((ResourceRef) msg).getResourceType());
			
			if(msg instanceof NewResource) {
				resources.add(((ResourceRef) msg).getResource());
			} else if(msg instanceof ResourceDestroyed) {
				resources.remove(((ResourceRef) msg).getResource());
			}			
		} else if(msg instanceof GetResources) {
			Object resourceType = ((GetResources) msg).getResourceType();
			Set<Object> resources =  new HashSet<>(getResourcesOfType(resourceType));
			
			getSender().tell(resources, getSender());
		} else if(msg instanceof GetTree) {
			getContext().actorSelection("*").tell(msg, getSender());
		} else {
			unhandled(msg);
		}
	}
}
