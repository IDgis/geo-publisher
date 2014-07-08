package nl.idgis.publisher.utils;

import com.typesafe.config.Config;

public class ConfigUtils {

	public static Config getOptionalConfig(Config config, String path) {
		if(config != null && config.hasPath(path)) {
			return config.getConfig(path);
		} else {		
			return null;
		}
	}

	public static String getOptionalString(Config config, String path) {
		if(config != null && config.hasPath(path)) {
			return config.getString(path);
		} else {		
			return null;
		}
	}
}
