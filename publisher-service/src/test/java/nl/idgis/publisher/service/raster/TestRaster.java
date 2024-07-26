package nl.idgis.publisher.service.raster;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

public class TestRaster {
	
	private static final String RASTER_FOLDER = "/var/lib/geo-publisher/raster";
	
	public static String getRasterUrlGeoServerContainer() {
		return Paths.get(RASTER_FOLDER, "gemiddeld_laagste_grondwaterstand.tif").toString();
	}
	
	public static URL getRasterUrl() {
		return TestRaster.class.getClassLoader().getResource("nl/idgis/publisher/service/raster/gemiddeld_laagste_grondwaterstand.tif");
	}

	public static String getRasterFolder() throws IOException, URISyntaxException {
		return RASTER_FOLDER;
	}
}
