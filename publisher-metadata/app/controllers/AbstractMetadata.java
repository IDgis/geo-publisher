package controllers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import play.Logger;
import router.dav.SimpleWebDAV;

import util.InetFilter;
import util.MetadataConfig;
import util.QueryDSL;

import static play.mvc.Controller.request;

public abstract class AbstractMetadata extends SimpleWebDAV {
	
	protected final WebJarAssets webJarAssets;
	
	protected final InetFilter filter;
	
	protected final MetadataConfig config;
	
	protected final QueryDSL q;
	
	protected AbstractMetadata(WebJarAssets webJarAssets, InetFilter filter, MetadataConfig config, QueryDSL q, String prefix) {
		super(prefix);
		
		this.webJarAssets = webJarAssets;
		this.filter = filter;
		this.config = config;
		this.q = q;
	}
	
	protected String getName(String id) {
		return id + ".xml";
	}
	
	protected Optional<String> getId(String name) {
		if(name.toLowerCase().endsWith(".xml")) {
			return Optional.of(name.substring(0, name.length() - 4));
		} else {
			return Optional.empty();
		}
	}
	
	protected static enum ServiceType {
		
		WMS("download", "GetFeature"), WFS("view", "GetMap");
		
		private final String name;
		
		private final String operationName;
		
		ServiceType(String name, String operationName) {
			this.name = name;
			this.operationName = operationName;
		}
		
		String getProtocol() {
			return "OGC:" + name();
		}
		
		String getName() {
			return name;
		}
		
		String getOperationName() {
			return operationName;
		}
	}
	
	protected String getServiceLinkage(String environmentId, String serviceName, ServiceType serviceType) {
		return config.getEnvironmentUrlPrefix(environmentId).orElse("/") + serviceName + "/" + serviceType.name().toLowerCase();
	}
	
	protected boolean isTrusted() {		
		try {
			InetAddress inetAddress = InetAddress.getByName(request().remoteAddress());
			return filter.isAllowed(inetAddress);
		} catch(UnknownHostException e) {
			Logger.warn("Couldn't determine if request is from a trusted address", e);
		}
		
		return false;
	}
}
