package nl.idgis.publisher.service.provisioning;

import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.service.geoserver.messages.EnsureTarget;

public interface ProvisioningPropsFactory {
	
	Props environmentInfoProviderProps(ActorRef database);

	Props serviceProps(ServiceInfo serviceInfo, String schema);
	
	Props ensureJobProps(Set<EnsureTarget> targets);
}
