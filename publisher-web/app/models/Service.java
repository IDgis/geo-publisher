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
		StringBuilder url = getGeoserverServiceUrlPrefix(serviceId);
		url.append("/wms?service=WMS&request=GetCapabilities&version=1.3.0");
		return url.toString();
	}
	
	/**
	 * Get a WFS GetCapabilities url for a specific service.
	 * @param serviceId
	 * @return url as a String 
	 */
	public static String getWfsGetCapUrl(String serviceId){
		StringBuilder url = getGeoserverServiceUrlPrefix(serviceId);
		url.append("/wfs?service=WFS&request=GetCapabilities&version=1.1.0");
		return url.toString();
	}
	
	/**
	 * Get the wmts getcapabilities url. 
	 * e.g. <code>http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetCapabilities</code> 
	 * @return wmts getcap url
	 */
	public static String getWmtsGetCapUrl(){
		StringBuilder url = getGeoserverUrlPrefix();
		url.append("/gwc/service/wmts?REQUEST=GetCapabilities");
		return url.toString();
	}
	
	/**
	 * Get the TMS getcapabilities url. 
	 * e.g. <code>http://localhost:8080/geoserver/gwc/service/tms/1.0.0</code>
	 * @return tms getcap url
	 */
	public static String getTmsGetCapUrl(){
		StringBuilder url = getGeoserverUrlPrefix();
		url.append("/gwc/service/tms/1.0.0");
		return url.toString();
	}
	
	/** 
	 * Url of a specific Geoserver service
	 * @param serviceId of the service
	 * @return url prefix of the geoserver service
	 */
	private static StringBuilder getGeoserverServiceUrlPrefix(String serviceId){
		StringBuilder url = getGeoserverUrlPrefix();
		url.append("/");
		url.append(serviceId);
		return url;
	}
	
	/**
	 * Url of the geoserver domain
	 * @return String geoserver url
	 */
	private static StringBuilder getGeoserverUrlPrefix(){
		StringBuilder url = new StringBuilder();
		url.append("https://");
		url.append(getConfig("publisher.preview.geoserverDomain"));
		url.append("/geoserver");
		return url;
	}
	
	private static String getConfig(String configKey){
		return Play.application().configuration().getString(configKey);
	}

}
