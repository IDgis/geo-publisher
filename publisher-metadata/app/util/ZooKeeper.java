package util;

import java.util.Collections;

import javax.inject.Inject;

import nl.idgis.sys.provisioning.domain.ProxyDomain;
import nl.idgis.sys.provisioning.domain.ProxyMapping;
import nl.idgis.sys.provisioning.domain.ProxyMappingType;
import nl.idgis.sys.provisioning.registration.ServiceRegistration;

import play.Logger;
import play.libs.F.Promise;
import play.inject.ApplicationLifecycle;

public class ZooKeeper {

	@Inject
	public ZooKeeper(MetadataConfig config, ApplicationLifecycle applicationLifecycle) {
		if(!config.getZooKeeperHosts().isPresent()) {
			Logger.info("ZooKeeper not configured");
		}
		
		config.getZooKeeperHosts().ifPresent(zooKeeperHosts -> {
			try {
				ServiceRegistration serviceRegistration = new ServiceRegistration(zooKeeperHosts, config.getZooKeeperNamespace().orElse(null));
				
				String host = config.getHost();
				Logger.info("Registering proxy domain: {}", host);
				serviceRegistration.registerProxyDomain(new ProxyDomain(
						config.getHost(),
						false, // supportHttps
						Collections.emptyList(), // domainAliases
						Collections.emptyMap() // additionalConfig
					));
				
				String path = config.getPath();
				String dest = "http://" + ServiceRegistration.getPublicIp() + ":9000" + config.getPath(); 
				Logger.info("Registering proxy mapping, domain: {}, path: {}, dest: {}", host, path, dest);
				serviceRegistration.registerProxyMapping(new ProxyMapping(
						config.getHost(),
						config.getPath(),
						dest,
						ProxyMappingType.HTTP
					));
	
				applicationLifecycle.addStopHook(() -> {
					try {
						serviceRegistration.close();
					} catch(Exception e) {
						Logger.error("Failed to terminate ZooKeeper registration", e);
					}
					return Promise.pure (null);
				});
				
				Logger.info("Registering has been successful");
			} catch(Exception e) {
				Logger.error("Failed to create ZooKeeper registration", e);
			}
		});
	}
}
