package models;

import play.Play;

/**
 * Helper class for service specific functionality.<br>
 * Contains methods for retrieving GetCapabilities url's.
 * @author Rob
 *
 */
public class Service {
	
	/**
	 * Get a WMS GetCapabilities url for a specific service.
	 * @param serviceId
	 * @return url as a String 
	 */
	public static String getWmsGetCapUrl(String serviceId){
		StringBuilder url = makeGetCapUrlPrefix(serviceId);
		url.append("/wms?service=WMS&request=GetCapabilities&version=1.3.0");
		return url.toString();
	}
	
	/**
	 * Get a WFS GetCapabilities url for a specific service.
	 * @param serviceId
	 * @return url as a String 
	 */
	public static String getWfsGetCapUrl(String serviceId){
		StringBuilder url = makeGetCapUrlPrefix(serviceId);
		url.append("/wfs?service=WFS&request=GetCapabilities&version=1.1.0");
		return url.toString();
	}
	
	private static String getConfig(String configKey){
		return Play.application().configuration().getString(configKey);
	}
	
	private static StringBuilder makeGetCapUrlPrefix(String serviceId){
		StringBuilder url = new StringBuilder();
		url.append("http://");
		url.append(getConfig("publisher.preview.geoserverDomain"));
		url.append("/geoserver/");
		url.append(serviceId);
		return url;
	}
	

}
