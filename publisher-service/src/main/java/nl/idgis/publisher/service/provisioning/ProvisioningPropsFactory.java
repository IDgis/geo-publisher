package nl.idgis.publisher.service.provisioning;

import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.job.manager.messages.ServiceJobInfo;

public interface ProvisioningPropsFactory {

	Props serviceProps(ServiceInfo serviceInfo, String schema);
	
	Props jobProps(ActorRef database, ActorRef serviceManager, ServiceJobInfo serviceJobInfo, ActorRef sender, Set<ActorRef> targets);
}
