package nl.idgis.publisher.service.geoserver.rest;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface GeoServerRest extends Closeable {

	CompletableFuture<Void> postDataStore(Workspace workspace, DataStore dataStore);

	CompletableFuture<Void> postFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType);
	
	CompletableFuture<Void> putFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType);
	
	CompletableFuture<Void> postLayerGroup(Workspace workspace, LayerGroup layerGroup);
	
	CompletableFuture<Void> putLayerGroup(Workspace workspace, LayerGroup layerGroup);

	CompletableFuture<Void> postWorkspace(Workspace workspace);
	
	CompletableFuture<Void> deleteDataStore(Workspace workspace, DataStore dataStore);

	CompletableFuture<Void> deleteFeatureType(Workspace workspace, DataStore dataStore, FeatureType featureType);
	
	CompletableFuture<Void> deleteLayerGroup(Workspace workspace, LayerGroup layerGroup);

	CompletableFuture<Void> deleteWorkspace(Workspace workspace);

	CompletableFuture<List<DataStore>> getDataStores(Workspace workspace);
	
	CompletableFuture<Optional<DataStore>> getDataStore(Workspace workspace, String dataStoreId);

	CompletableFuture<List<FeatureType>> getFeatureTypes(Workspace workspace, DataStore dataStore);
	
	CompletableFuture<List<LayerGroup>> getLayerGroups(Workspace workspace);

	CompletableFuture<List<Workspace>> getWorkspaces();
	
	CompletableFuture<Optional<Workspace>> getWorkspace(String workspaceId);

	CompletableFuture<Void> putServiceSettings(Workspace workspace, ServiceType serviceType, ServiceSettings serviceSettings);
	
	CompletableFuture<Optional<ServiceSettings>> getServiceSettings(Workspace workspace, ServiceType serviceType);
	
	CompletableFuture<Void> putWorkspaceSettings(Workspace workspace, WorkspaceSettings workspaceSettings);

	CompletableFuture<WorkspaceSettings> getWorkspaceSettings(Workspace workspace);
	
	CompletableFuture<Optional<Style>> getStyle(String styleId);
	
	CompletableFuture<List<Style>> getStyles();
	
	CompletableFuture<Void> postStyle(Style style);
	
	CompletableFuture<Void> putStyle(Style style);
	
	CompletableFuture<Layer> getLayer(Workspace workspace, FeatureType featureType);
	
	CompletableFuture<Layer> getLayer(Workspace workspace, Coverage coverage);
	
	CompletableFuture<Void> putLayer(Workspace workspace, Layer layer);
	
	CompletableFuture<Void> deleteStyle(Style style);
	
	CompletableFuture<Optional<TiledLayer>> getTiledLayer(Workspace workspace, FeatureType featureType);
	
	CompletableFuture<Optional<TiledLayer>> getTiledLayer(Workspace workspace, LayerGroup layerGroup);
	
	CompletableFuture<Optional<TiledLayer>> getTiledLayer(Workspace workspace, String layerName);
	
	CompletableFuture<List<String>> getTiledLayerNames(Workspace workspace);
	
	CompletableFuture<Void> deleteTiledLayer(Workspace workspace, FeatureType featureType);
	
	CompletableFuture<Void> deleteTiledLayer(Workspace workspace, LayerGroup layerGroup);
	
	CompletableFuture<Void> deleteTiledLayer(Workspace workspace, String layerName);
	
	CompletableFuture<Void> putTiledLayer(Workspace workspace, String layerName, TiledLayer tiledLayer);
	
	CompletableFuture<Void> postTiledLayer(Workspace workspace, String layerName, TiledLayer tiledLayer);

	CompletableFuture<Void> postCoverageStore(Workspace workspace, CoverageStore coverageStore);

	CompletableFuture<Void> postCoverage(Workspace workspace, CoverageStore coverageStore, Coverage coverage);

	CompletableFuture<List<Coverage>> getCoverages(Workspace workspace, CoverageStore coverageStore);

	CompletableFuture<Map<CoverageStore, List<Coverage>>> getCoverages(Workspace workspace);

	CompletableFuture<Void> putCoverageStore(Workspace workspace, CoverageStore coverageStore);

	CompletableFuture<Void> putCoverage(Workspace workspace, CoverageStore coverageStore, Coverage coverage);

	CompletableFuture<Void> deleteCoverageStore(Workspace workspace, CoverageStore coverageStore);
}
