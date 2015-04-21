package nl.idgis.publisher.service.raster;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class TestRaster {
	
	public static URL getRasterUrl() {
		return TestRaster.class.getClassLoader().getResource("nl/idgis/publisher/service/raster/bogota.tif");
	}

	public static String getRasterFolder() throws IOException, URISyntaxException {
		return new File(getRasterUrl().toURI ().getPath ()).getParentFile().getCanonicalPath();
	}
}
