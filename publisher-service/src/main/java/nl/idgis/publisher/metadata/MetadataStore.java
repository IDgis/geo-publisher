package nl.idgis.publisher.metadata;

import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

public interface MetadataStore {

	Future<Void> deleteAll();
	Future<Void> put(String name, MetadataDocument metadataDocument, ExecutionContext executionContext);
	Future<MetadataDocument> get(String name, ExecutionContext executionContext);
}
