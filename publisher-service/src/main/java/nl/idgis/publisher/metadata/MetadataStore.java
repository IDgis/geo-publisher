package nl.idgis.publisher.metadata;

import java.util.concurrent.CompletableFuture;

public interface MetadataStore {

	CompletableFuture<Void> deleteAll();
	CompletableFuture<Void> put(String name, MetadataDocument metadataDocument);
	CompletableFuture<MetadataDocument> get(String name);
}
