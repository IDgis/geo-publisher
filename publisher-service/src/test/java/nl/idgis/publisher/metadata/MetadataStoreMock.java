package nl.idgis.publisher.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.utils.FutureUtils;

public class MetadataStoreMock implements MetadataStore {
	
	private final Map<String, MetadataDocument> content;
	
	private final FutureUtils f;
	
	public MetadataStoreMock(FutureUtils f) {
		content = new HashMap<>();
		this.f = f;
	}

	@Override
	public CompletableFuture<Void> deleteAll() {
		content.clear();
		
		return f.successful(null);
	}

	@Override
	public CompletableFuture<Void> put(String name, MetadataDocument metadataDocument) {
		content.put(name,  metadataDocument);
		
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

}
