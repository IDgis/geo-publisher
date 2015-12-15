package nl.idgis.publisher.service.provisioning;

import java.util.Set;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.service.geoserver.GeoServerService;
import nl.idgis.publisher.service.geoserver.InfoCollector;
import nl.idgis.publisher.service.geoserver.messages.EnsureTarget;
import nl.idgis.publisher.service.geoserver.rest.DefaultGeoServerRestFactory;
import nl.idgis.publisher.service.geoserver.rest.GeoServerRestFactory;

public class DefaultProvisioningPropsFactory implements ProvisioningPropsFactory {

	@Override
	public Props serviceProps(ServiceInfo serviceInfo, String schema) {
		ConnectionInfo serviceConnectionInfo = serviceInfo.getService();
		
		return GeoServerService.props(
			restFactory(serviceConnectionInfo),	
			serviceInfo.getRasterFolder(),
			schema);
	}

	protected GeoServerRestFactory restFactory(ConnectionInfo serviceConnectionInfo) {
		return new DefaultGeoServerRestFactory(
			serviceConnectionInfo.getUrl(), 
			serviceConnectionInfo.getUser(), 
			serviceConnectionInfo.getPassword());
	}

	@Override
	public Props ensureJobProps(Set<EnsureTarget> targets) {
		return InfoCollector.props(targets);
	}

	@Override
	public Props environmentInfoProviderProps(ActorRef database) {
		return EnvironmentInfoProvider.props(database);
	}
}
