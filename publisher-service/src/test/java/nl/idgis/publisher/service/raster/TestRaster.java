package nl.idgis.publisher.service.raster;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class TestRaster {
	
	public static URL getRasterUrl() {
		return TestRaster.class.getClassLoader().getResource("nl/idgis/publisher/service/raster/bogota.tif");
	}

	public static String getRasterFolder() throws IOException {
		return new File(getRasterUrl().getFile()).getParentFile().getCanonicalPath();
	}
}
