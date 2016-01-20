package controller;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import router.dav.SimpleWebDAV;

import util.QueryDSL;

public abstract class AbstractMetadata extends SimpleWebDAV {
	
	protected final QueryDSL q;
	
	protected AbstractMetadata(QueryDSL q, String prefix) {
		super(prefix);
		
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
	
	protected String getServiceName(String jsonService) {
		JsonNode jsonNode = Json.parse(jsonService);
		return jsonNode.get("name").asText();
	}
	
	protected String getServiceLinkage(String serviceName, ServiceType serviceType) {
		// TODO: add prefix
		return "" + serviceName + "/" + serviceType.name().toLowerCase();
	}
}
