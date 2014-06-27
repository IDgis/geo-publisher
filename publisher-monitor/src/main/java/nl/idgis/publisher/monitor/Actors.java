package nl.idgis.publisher.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.idgis.publisher.monitor.messages.GetResources;
import nl.idgis.publisher.monitor.messages.GetTree;
import nl.idgis.publisher.monitor.messages.Node;
import nl.idgis.publisher.monitor.messages.ParentNode;
import nl.idgis.publisher.monitor.messages.Tree;
import nl.idgis.publisher.monitor.messages.ValueNode;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Actors extends UntypedActor {
	
	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	private final ActorRef monitor;
	
	public Actors(ActorRef monitor) {
		this.monitor = monitor;
	}
	
	public static Props props(ActorRef monitor) {
		return Props.create(Actors.class, monitor);
	}
	
	@Override
	public void preStart() {
		log.debug("actors tree generator started");
	}
	
	private Node getChild(Map<String, Set<String>> parentChild, Map<String, Object> values, String child, String path) {
		Object value = values.get(path);
		
		if(parentChild.containsKey(child)) {
			return new ParentNode(child, value, getChildren(parentChild, values, child, path));
		} else {
			return new ValueNode(child, value);
		}
	}
	
	
	private Node[] getChildren(Map<String, Set<String>> parentChild, Map<String, Object> values, String parent, String path) {
		if(parentChild.containsKey(parent)) {
			List<String> children = new ArrayList<String>(parentChild.get(parent));
			Collections.sort(children);
			
			int i = 0;
			Node[] retval = new Node[children.size()];
			
			for(String child : children) {
				retval[i++] = getChild(parentChild, values, child, path + "/" + child);
			}
			
			return retval;
		} else {
			return new Node[0];
		}
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GetTree) {
			monitor.tell(new GetResources(UntypedActor.class), getSelf());
		} else if(msg instanceof Set) {
			@SuppressWarnings("unchecked")
			Set<ActorRef> actorRefs = (Set<ActorRef>)msg;
			
			Map<String, Object> values = new HashMap<>();
			Map<String, Set<String>> parentChild = new HashMap<>();
			
			for(ActorRef actorRef : actorRefs) {
				String path = actorRef.path().toStringWithoutAddress();
				
				values.put(path, actorRef);
				
				String[] pathItems = path.substring(1).split("/");
				for(int i = 0; i < pathItems.length - 1; i++) {
					String parent = pathItems[i], child = pathItems[i + 1];
					
					Set<String> children;
					if(parentChild.containsKey(parent)) {
						children = parentChild.get(parent);
					} else {
						children = new HashSet<>();
						parentChild.put(parent, children);
					}
					
					children.add(child);
				}
				
				Tree actorTree = new Tree("Actors", getChildren(parentChild, values, "user", "/user")); 
				getSender().tell(actorTree, getSelf());
			}
		} else {
			unhandled(msg);
		}
	}
	
}
