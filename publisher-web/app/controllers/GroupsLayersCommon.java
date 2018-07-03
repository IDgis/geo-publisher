package controllers;

import actions.DefaultAuthenticator;
import play.Play;
import play.mvc.Controller;
import play.mvc.Security;

@Security.Authenticated (DefaultAuthenticator.class)
public class GroupsLayersCommon extends Controller {
	
	protected static String getConfig(String configKey){
		return Play.application().configuration().getString(configKey);
	}
	
	protected static String makePreviewUrl(String serviceId, String layerId){
		StringBuilder url = new StringBuilder();
		url.append(getConfig("publisher.viewer.prefix"));
		url.append("/layer/" + serviceId);
		url.append("/" + layerId);
		
		return url.toString();
	}
	
	protected static String makeOldPreviewUrl(String serviceId, String layerId){
		StringBuilder url = new StringBuilder();
		url.append("https://");
		url.append(getConfig("publisher.preview.geoserverDomain"));
		url.append(getConfig ("publisher.preview.geoserverPath"));
		url.append("/" + serviceId);
		url.append("/wms?");
		url.append(getConfig("publisher.preview.serviceRequest"));
		url.append("&");
		url.append("layers=" + serviceId + ":"+ layerId);
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