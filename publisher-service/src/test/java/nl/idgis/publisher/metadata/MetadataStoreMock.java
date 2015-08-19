package nl.idgis.publisher.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.utils.FutureUtils;

public class MetadataStoreMock implements MetadataStore {
	
	private final Set<String> overwritten;
	
	private final Map<String, MetadataDocument> content;
	
	private final FutureUtils f;
	
	public MetadataStoreMock(FutureUtils f) {
		this.f = f;
		
		content = new HashMap<>();
		overwritten = new HashSet<>();
	}

	@Override
	public CompletableFuture<Void> deleteAll() {
		content.clear();
		
		return f.successful(null);
	}

	@Override
	public CompletableFuture<Void> put(String name, MetadataDocument metadataDocument) {
		if(content.put(name,  metadataDocument) != null) {
			overwritten.add(name);
		}
		
		return f.successful(null);
	}

	@Override
	public CompletableFuture<MetadataDocument> get(String name) {
		if(content.containsKey(name)) {
			return f.successful(content.get(name));
		} else {
			return f.failed(new NoSuchElementException());
		}
	}
	
	Set<String> getOverwritten() {
		return overwritten;
	}

}
