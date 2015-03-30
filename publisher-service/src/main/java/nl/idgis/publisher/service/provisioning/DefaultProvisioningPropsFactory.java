package nl.idgis.publisher.service.provisioning;

import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.service.geoserver.GeoServerService;
import nl.idgis.publisher.service.geoserver.InfoCollector;

public class DefaultProvisioningPropsFactory implements ProvisioningPropsFactory {

	@Override
	public Props serviceProps(ServiceInfo serviceInfo, String schema) {
		ConnectionInfo serviceConnectionInfo = serviceInfo.getService();
		ConnectionInfo databaseConnectionInfo = serviceInfo.getDatabase();
		
		return GeoServerService.props(
			serviceConnectionInfo.getUrl(), 
			serviceConnectionInfo.getUser(), 
			serviceConnectionInfo.getPassword(), 
			
			databaseConnectionInfo.getUrl(), 
			databaseConnectionInfo.getUser(), 
			databaseConnectionInfo.getPassword(), 
			
			schema);
	}

	@Override
	public Props ensureJobProps(Set<ActorRef> targets) {
		return InfoCollector.props(targets);
	}
}
