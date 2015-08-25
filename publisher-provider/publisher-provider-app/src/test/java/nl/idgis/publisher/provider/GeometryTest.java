package nl.idgis.publisher.provider;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.deegree.geometry.Geometry;
import org.deegree.geometry.GeometryFactory;
import org.deegree.geometry.io.WKBReader;
import org.deegree.geometry.io.WKBWriter;
import org.junit.Test;

public class GeometryTest {

	@Test
	public void testCurve() throws Exception {
		GeometryFactory factory = new GeometryFactory();
		
		Geometry inputGeometry = factory.createCurve(null, null,
			factory.createArcString(factory.createPoints(Arrays.asList(
				factory.createPoint(null, new double[]{0, 0}, null),
				factory.createPoint(null, new double[]{1, 1}, null),
				factory.createPoint(null, new double[]{1, 0}, null)))));
		
		byte[] wkb = WKBWriter.write(inputGeometry);
		
		Geometry outputGeometry = WKBReader.read(wkb, null);
		assertNotNull(outputGeometry);
	}
}
