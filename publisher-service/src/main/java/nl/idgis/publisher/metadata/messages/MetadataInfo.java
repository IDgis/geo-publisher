package nl.idgis.publisher.metadata.messages;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QGenericLayer;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;

import nl.idgis.publisher.service.json.JsonService;
import nl.idgis.publisher.utils.StreamUtils;
import nl.idgis.publisher.utils.TypedList;

public class MetadataInfo implements Serializable {	

	private static final long serialVersionUID = -4466982583083563284L;
	
	public static final QGenericLayer layerGenericLayer = new QGenericLayer("layerGenericLayer");

	public static final QGenericLayer serviceGenericLayer = new QGenericLayer("serviceGenericLayer");

	private final List<Tuple> tuples;
	
	private final Map<String, Set<String>> datasetServices, datasetLayers;

	private final Map<String, Map<String, Set<String>>> serviceLayerLayerName;

	public MetadataInfo(List<Tuple> tuples) {
		this.tuples = Objects.requireNonNull(tuples, "tuples must not be null");
		
		serviceLayerLayerName = 
			tuples.stream()
				.map(StreamUtils.wrap(tuple -> tuple.get(serviceGenericLayer.identification)))
				.distinct()
				.map(StreamUtils.Wrapper::unwrap)
				.map(tuple -> tuple.get(publishedService.content))
				.map(JsonService::fromJson)
				.collect(Collectors.toMap(
						service -> service.getId(),
						service -> traverseLayers(new HashMap<>(), service.getLayers())));
		
		datasetServices =
			tuples.stream()
				.collect(Collectors.groupingBy(
					tuple -> tuple.get(dataset.identification),
					Collectors.mapping(
						tuple -> tuple.get(serviceGenericLayer.identification),
						Collectors.toSet())));
		
		datasetLayers = 
			tuples.stream()
				.collect(Collectors.groupingBy(
					tuple -> tuple.get(dataset.identification),
					Collectors.mapping(
						tuple -> tuple.get(layerGenericLayer.identification),
						Collectors.toSet())));
	}	
	
	private static Map<String, Set<String>> traverseLayers(Map<String, Set<String>> layerInfo, List<LayerRef<? extends Layer>> layerRefs) {
		for(LayerRef<? extends Layer> layerRef : layerRefs) {
			if(layerRef.isGroupRef()) {
				GroupLayer groupLayer = layerRef.asGroupRef().getLayer();
				traverseLayers(layerInfo, groupLayer.getLayers());
			} else {
				Layer layer = layerRef.getLayer();

				Set<String> layerNames;
				String layerId = layer.getId();
				if(layerInfo.containsKey(layerId)) {
					layerNames = layerInfo.get(layerId);
				} else {
					layerNames = new HashSet<>();
					layerInfo.put(layerId, layerNames);
				}
				
				layerNames.add(layer.getName());
			}
		}
		
		return layerInfo;
	}
	
	public static CompletableFuture<TypedList<Tuple>> fetch(AsyncSQLQuery query) {
		return query.from(publishedService)
			.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
			.join(service).on(service.id.eq(publishedServiceDataset.serviceId))
			.join(serviceGenericLayer).on(serviceGenericLayer.id.eq(service.genericLayerId))
			.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
			.join(leafLayer).on(leafLayer.datasetId.eq(dataset.id))
			.join(layerGenericLayer).on(layerGenericLayer.id.eq(leafLayer.genericLayerId))
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
			.list(
				publishedService.content,
				serviceGenericLayer.identification,
				layerGenericLayer.identification,
				dataset.identification,
				dataset.uuid,
				dataset.fileUuid,
				sourceDataset.externalIdentification,
				dataSource.identification);
	}
	
	private Set<String> getServiceIds(String datasetId) {
		if(datasetServices.containsKey(datasetId)) {			
			return datasetServices.get(datasetId);
		} else {
			throw new IllegalStateException("no services for dataset: " + datasetId);
		}
	}
	
	private Set<String> getLayerIds(String datasetId) {
		if(datasetLayers.containsKey(datasetId)) {			
			return datasetLayers.get(datasetId);
		} else {
			throw new IllegalStateException("no layers for dataset: " + datasetId);
		}
	}
	
	private Map<String, Set<String>> getLayerNames(String serviceId) {
		if(serviceLayerLayerName.containsKey(serviceId)) {
			return serviceLayerLayerName.get(serviceId);
		} else {
			throw new IllegalStateException("no layers for service: " + serviceId);
		}
	}
	
	public Iterator<DatasetInfo> getDatasets() {
		return 
			tuples.stream()
				.map(StreamUtils.wrap(tuple -> tuple.get(dataset.identification)))
				.distinct()
				.map(StreamUtils.Wrapper::unwrap)
				.map(tuple -> { 
					String datasetId = tuple.get(dataset.identification);								
								
					Set<String> serviceIds = getServiceIds(datasetId);
					Set<String> layerIds = getLayerIds(datasetId);
					
					Set<ServiceRef> serviceRefs =
						serviceIds.stream()
							.flatMap(serviceId ->
								getLayerNames(serviceId).entrySet().stream()
									.filter(entry -> layerIds.contains(entry.getKey()))
									.map(entry -> new ServiceRef(serviceId, entry.getValue())))
							.collect(Collectors.toSet());
					
					return new DatasetInfo(
						datasetId, 
						tuple.get(dataSource.identification),
						tuple.get(sourceDataset.externalIdentification),
						tuple.get(dataset.uuid),
						tuple.get(dataset.fileUuid),
						serviceRefs);
				})
				.iterator();
	}
	
	public Iterator<ServiceInfo> getServices() {
		return tuples.stream()
			.collect(Collectors.groupingBy(
				tuple -> tuple.get(serviceGenericLayer.identification),
				Collectors.mapping(
					tuple -> {
						String serviceId = tuple.get(serviceGenericLayer.identification);
						String datasetId = tuple.get(dataset.identification);
	
						Map<String, Set<String>> layerNames = getLayerNames(serviceId);
						
						return new DatasetRef(
							datasetId,
							tuple.get(dataset.uuid),
							tuple.get(dataset.fileUuid),
							getLayerIds(datasetId).stream()
								.flatMap(layerId -> {
									if(layerNames.containsKey(layerId)) {
										return layerNames.get(layerId).stream();
									} else {
										throw new IllegalStateException("no layerNames for layer: " + layerId);
									}
								})
								.collect(Collectors.toSet())); 	
					},
					Collectors.toSet()))).entrySet().stream()
						.map(entry -> new ServiceInfo(entry.getKey(), entry.getValue()))
						.iterator();
	}

	@Override
	public String toString() {
		return "MetadataInfo [tuples=" + tuples + "]";
	}
}
