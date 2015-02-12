package nl.idgis.publisher.service.geoserver.rest;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GeoServerRest extends Closeable {

	CompletableFuture<Void> addDataStore(Workspace workspace, DataStore dataStore);

	CompletableFuture<Void> addFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType);

	CompletableFuture<Void> addWorkspace(Workspace workspace);

	CompletableFuture<List<CompletableFuture<DataStore>>> getDataStores(Workspace workspace);

	CompletableFuture<List<CompletableFuture<FeatureType>>> getFeatureTypes(Workspace workspace, DataStore dataStore);

	CompletableFuture<List<Workspace>> getWorkspaces();

}
