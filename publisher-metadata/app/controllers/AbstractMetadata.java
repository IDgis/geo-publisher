package controllers;

import java.net.UnknownHostException;
import java.util.Optional;

import play.Logger;
import nl.idgis.dav.router.SimpleWebDAV;

import util.MetadataConfig;
import util.QueryDSL;

import static play.mvc.Controller.request;

public abstract class AbstractMetadata extends SimpleWebDAV {
	
	protected final WebJarAssets webJarAssets;
	
	protected final MetadataConfig config;
	
	protected final QueryDSL q;
	
	protected AbstractMetadata(WebJarAssets webJarAssets, MetadataConfig config, QueryDSL q, String prefix) {
		super(prefix);
		
		this.webJarAssets = webJarAssets;
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
		
		WFS("download", "GetFeature"), WMS("view", "GetMap");
		
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
	
	protected String getServiceLinkage(String environmentUrl, String serviceName, ServiceType serviceType) {
		return environmentUrl + serviceName + "/" + serviceType.name().toLowerCase();
	}
	
	protected boolean isTrusted() {
		String trustedHeaderName = config.getTrustedHeader();
		String trustedHeader = request().getHeader(trustedHeaderName);
		
		return "1".equals(trustedHeader);
	}
}
