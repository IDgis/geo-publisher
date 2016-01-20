package controller;

import java.util.Optional;

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
}
