package nl.idgis.publisher.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.FileSystem;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class MetadataConfigTest {

	@Test
	public void testLoadConfig() {
		Config config = ConfigFactory.load("application.conf");
		
		FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
		MetadataConfig metadataConfig = new MetadataConfig(config.getConfig("publisher.service.metadata"), fileSystem);
		
		assertEquals(fileSystem.getPath("/source/metadata/service"), metadataConfig.getServiceMetadataSource());
		
		Map<String, MetadataEnvironmentConfig> environments =
			metadataConfig.getEnvironments().stream()
				.collect(Collectors.toMap(
					MetadataEnvironmentConfig::getName,
					Function.identity()));
		
		assertTrue(environments.containsKey("geoserver-public"));
		assertTrue(environments.containsKey("geoserver-secure"));
		assertTrue(environments.containsKey("geoserver-guaranteed"));
		
		MetadataEnvironmentConfig environment = environments.get("geoserver-public");
		assertNotNull(environment);
		
		assertEquals("http://public.example.com/geoserver/", environment.getServiceLinkagePrefix());
		assertEquals("http://public.example.com/metadata/dataset/", environment.getDatasetMetadataPrefix());
		assertEquals(fileSystem.getPath("/target/metadata/service/geoserver-public"), environment.getServiceMetadataTarget());
		assertEquals(fileSystem.getPath("/target/metadata/dataset/geoserver-public"), environment.getDatasetMetadataTarget());
	}
}
