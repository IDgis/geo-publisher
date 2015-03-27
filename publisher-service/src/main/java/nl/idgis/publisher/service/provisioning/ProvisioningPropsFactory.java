package nl.idgis.publisher.service.provisioning;

import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;

public interface ProvisioningPropsFactory {

	Props serviceProps(ServiceInfo serviceInfo, String schema);
	
	Props ensureJobProps(Set<ActorRef> targets);
}
