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
}