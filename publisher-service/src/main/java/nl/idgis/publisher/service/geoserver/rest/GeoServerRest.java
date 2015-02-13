package nl.idgis.publisher.service.geoserver.rest;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GeoServerRest extends Closeable {

	CompletableFuture<Void> postDataStore(Workspace workspace, DataStore dataStore);

	CompletableFuture<Void> postFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType);
	
	CompletableFuture<Void> postLayerGroup(Workspace workspace, LayerGroup layerGroup);

	CompletableFuture<Void> postWorkspace(Workspace workspace);
	
	CompletableFuture<Void> deleteDataStore(Workspace workspace, DataStore dataStore);

	CompletableFuture<Void> deleteFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType);
	
	CompletableFuture<Void> deleteLayerGroup(Workspace workspace, LayerGroup layerGroup);

	CompletableFuture<Void> deleteWorkspace(Workspace workspace);

	CompletableFuture<List<CompletableFuture<DataStore>>> getDataStores(Workspace workspace);

	CompletableFuture<List<CompletableFuture<FeatureType>>> getFeatureTypes(Workspace workspace, DataStore dataStore);
	
	CompletableFuture<List<CompletableFuture<LayerGroup>>> getLayerGroups(Workspace workspace);

	CompletableFuture<List<Workspace>> getWorkspaces();

}
