package controllers;

import play.Play;
import play.mvc.Controller;
import play.mvc.Security;
import actions.DefaultAuthenticator;

@Security.Authenticated (DefaultAuthenticator.class)
public class GroupsLayersCommon extends Controller {
	
	protected static String getConfig(String configKey){
		return Play.application().configuration().getString(configKey);
	}
	
	protected static String makePreviewUrl(String serviceId, String layerId){
		/*
		 * https://overijssel.geo-hosting.nl/geoserver/b2/wms?service=WMS&version=1.1.0&request=GetMap&layers=b2:strooizout&styles=&bbox=203383.0,475045.0,248661.0,519851.0&width=512&height=506&srs=EPSG:28992&format=application/openlayers
		 */

		StringBuilder url = new StringBuilder();
		url.append("http://");
		url.append(getConfig("publisher.preview.geoserverDomain"));
		url.append("/geoserver");
		url.append("/" + serviceId);
		url.append("/wms?");
		url.append(getConfig("publisher.preview.serviceRequest"));
		url.append("&");
		url.append("layers="  + serviceId + ":"+ layerId);
		url.append("&");
		url.append("styles=" + getConfig("publisher.preview.styles"));
		url.append("&");
		url.append("bbox=" + getConfig("publisher.preview.bbox"));
		url.append("&");
		url.append("width=" + getConfig("publisher.preview.width"));
		url.append("&");
		url.append("height=" + getConfig("publisher.preview.height"));
		url.append("&");
		url.append("srs=" + getConfig("publisher.preview.srs"));
		url.append("&");
		url.append("format=" + getConfig("publisher.preview.format"));
		
		return url.toString();
	}
	
	
}