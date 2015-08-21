package nl.idgis.publisher.metadata;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class MetadataConfigTest {

	@Test
	public void testLoadConfig() {
		Config config = ConfigFactory.load("application.conf");
		
		MetadataConfig metadataConfig = new MetadataConfig(config.getConfig("publisher.service.metadata"));
		
		assertNotNull(metadataConfig.getServiceMetadataSource());
		
		Set<String> environmentNames =
			metadataConfig.getEnvironments().stream()
				.map(environmentConfig -> { 
					environmentConfig.getDatasetMetadataTarget();
					environmentConfig.getServiceMetadataTarget();
					
					return environmentConfig.getName(); 
				})
				.collect(Collectors.toSet());
		
		assertNotNull(environmentNames);
		assertTrue(environmentNames.contains("geoserver-public"));
		assertTrue(environmentNames.contains("geoserver-secure"));
		assertTrue(environmentNames.contains("geoserver-guaranteed"));
	}
}
