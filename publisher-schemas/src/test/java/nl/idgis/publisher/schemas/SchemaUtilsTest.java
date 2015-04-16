package nl.idgis.publisher.schemas;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SchemaUtilsTest {

	@Test
	public void testSchema() throws Exception {
		assertNotNull(SchemaUtils.getSchema(SchemaRef.GEOSERVER_SLD));
	}
}
