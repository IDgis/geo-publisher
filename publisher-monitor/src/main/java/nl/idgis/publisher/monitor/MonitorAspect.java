package nl.idgis.publisher.monitor;

import nl.idgis.publisher.monitor.messages.NewResource;
import nl.idgis.publisher.monitor.messages.ResourceDestroyed;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

@Aspect
public class MonitorAspect {
	
	private ActorRef monitor;
	
	public void setMonitor(ActorRef monitor) {
		if(this.monitor != null) {
			throw new IllegalStateException("Monitor already set");
		}
		
		this.monitor = monitor;
	}
	
	private void tell(Object msg) {
		if(monitor != null) {
			monitor.tell(msg, ActorRef.noSender());
		}
	}
	
	private void newResource(Object resourceType, Object resource) {
		tell(new NewResource(resourceType, resource));
	}
	
	private void resourceDestroyed(Object resourceType, Object resource) {
		tell(new ResourceDestroyed(resourceType, resource));
	}

	@After("target(untypedActor) && call(* akka.actor.Actor.preStart(..))")
	public void preStart(UntypedActor untypedActor) {
		newResource(UntypedActor.class, untypedActor.getSelf());
	}
	
	@Before("target(untypedActor) && call(* akka.actor.Actor.postStop(..))")
	public void postStop(UntypedActor untypedActor) {
		resourceDestroyed(UntypedActor.class, untypedActor.getSelf());
	}
}
