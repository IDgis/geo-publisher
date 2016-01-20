package controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import router.dav.SimpleWebDAV;

import util.MetadataConfig;
import util.QueryDSL;

public abstract class AbstractMetadata extends SimpleWebDAV {
	
	protected final MetadataConfig config;
	
	protected final QueryDSL q;
	
	private Map<String, String> environmentPrefixes;
	
	protected AbstractMetadata(MetadataConfig config, QueryDSL q, String prefix) {
		super(prefix);
		
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
		
		WMS, WFS;
		
		String getProtocol() {
			return "OGC:" + name();
		}
	}
	
	protected String getServiceLinkage(String environmentId, String serviceName, ServiceType serviceType) {
		return config.getEnvironmentUrlPrefix(environmentId).orElse("/") + serviceName + "/" + serviceType.name().toLowerCase();
	}
}
