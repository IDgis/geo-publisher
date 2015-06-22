package nl.idgis.publisher.service.provisioning;

import java.util.Set;

import com.ning.http.client.AsyncHttpClient;

import akka.actor.ActorRef;
import akka.actor.Props;

import nl.idgis.publisher.service.geoserver.GeoServerService;
import nl.idgis.publisher.service.geoserver.InfoCollector;

public class DefaultProvisioningPropsFactory implements ProvisioningPropsFactory {
	
	private final AsyncHttpClient asyncHttpClient;
	
	public DefaultProvisioningPropsFactory(AsyncHttpClient asyncHttpClient) {
		this.asyncHttpClient = asyncHttpClient;
	}

	@Override
	public Props serviceProps(ServiceInfo serviceInfo, String schema) {
		ConnectionInfo serviceConnectionInfo = serviceInfo.getService();
		ConnectionInfo databaseConnectionInfo = serviceInfo.getDatabase();
		
		return GeoServerService.props(
			asyncHttpClient,
				
			serviceConnectionInfo.getUrl(), 
			serviceConnectionInfo.getUser(), 
			serviceConnectionInfo.getPassword(), 
			
			databaseConnectionInfo.getUrl(), 
			databaseConnectionInfo.getUser(), 
			databaseConnectionInfo.getPassword(),
			
			serviceInfo.getRasterFolder(),
			
			schema);
	}

	@Override
	public Props ensureJobProps(Set<ActorRef> targets) {
		return InfoCollector.props(targets);
	}

	@Override
	public Props environmentInfoProviderProps(ActorRef database) {
		return EnvironmentInfoProvider.props(database);
	}
}
