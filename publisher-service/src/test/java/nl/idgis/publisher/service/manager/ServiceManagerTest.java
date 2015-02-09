package nl.idgis.publisher.service.manager;

import org.junit.Test;

import nl.idgis.publisher.service.manager.messages.GetService;
import nl.idgis.publisher.service.manager.messages.Service;
import nl.idgis.publisher.AbstractServiceTest;

public class ServiceManagerTest extends AbstractServiceTest {

	@Test
	public void testGetService() throws Exception {
		sync.ask(serviceManager, new GetService("service0"), Service.class);
	}
}
