package nl.idgis.publisher.schemas;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SchemaUtilsTest {

	@Test
	public void testSchema100() throws Exception {
		assertNotNull(SchemaUtils.getSchema(SchemaRef.GEOSERVER_SLD_1_0_0));
	}
	
	@Test
	public void testSchema110() throws Exception {
		assertNotNull(SchemaUtils.getSchema(SchemaRef.GEOSERVER_SLD_1_1_0));
	}
}
