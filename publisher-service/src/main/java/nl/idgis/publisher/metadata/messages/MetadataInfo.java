package nl.idgis.publisher.metadata.messages;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QEnvironment.environment;

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
import java.util.stream.Stream;

import com.mysema.query.Tuple;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.DatabaseUtils;
import nl.idgis.publisher.database.QGenericLayer;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;

import nl.idgis.publisher.metadata.MetadataInfoProcessor;
import nl.idgis.publisher.service.json.JsonService;
import nl.idgis.publisher.utils.StreamUtils;
import nl.idgis.publisher.utils.TypedList;

/**
 * Contains all dataset and service information required by {@link MetadataInfoProcessor}. 
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class MetadataInfo implements Serializable {	

	private static final long serialVersionUID = 140228953110487193L;

	private static final QGenericLayer layerGenericLayer = new QGenericLayer("layerGenericLayer");

	private static final QGenericLayer serviceGenericLayer = new QGenericLayer("serviceGenericLayer");	
	
	private final Stream<ServiceInfo> serviceInfo;
	
	private final Stream<DatasetInfo> datasetInfo;	

	private MetadataInfo(Stream<ServiceInfo> serviceInfo, Stream<DatasetInfo> datasetInfo) {		
		this.serviceInfo = Objects.requireNonNull(serviceInfo, "serviceInfo must not be null");
		this.datasetInfo = Objects.requireNonNull(datasetInfo, "datasetInfo must not be null");
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
	
	public static CompletableFuture<MetadataInfo> fetch(AsyncHelper tx, String environmentId) {
		CompletableFuture<TypedList<Tuple>> serviceInfoQuery = tx.query().from(publishedService)
			.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))
			.join(service).on(service.id.eq(publishedService.serviceId))
			.join(serviceGenericLayer).on(serviceGenericLayer.id.eq(service.genericLayerId))
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.where(environment.identification.eq(environmentId))
			.orderBy(publishedService.serviceId.asc())
			.list(
				publishedService.serviceId,
				publishedService.content,
				service.wfsMetadataFileIdentification,
				service.wmsMetadataFileIdentification,
				serviceGenericLayer.identification,
				serviceGenericLayer.name);
		
		CompletableFuture<TypedList<Tuple>> serviceDatasetInfoQuery = tx.query().from(publishedService)
			.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))
			.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
			.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
			.join(leafLayer).on(leafLayer.datasetId.eq(dataset.id))
			.join(layerGenericLayer).on(layerGenericLayer.id.eq(leafLayer.genericLayerId))
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.where(environment.identification.eq(environmentId))
			.orderBy(publishedService.serviceId.asc(), dataset.id.asc())
			.list(
				publishedService.serviceId,
				dataset.id,
				dataset.identification,
				dataset.metadataIdentification,
				dataset.metadataFileIdentification,
				layerGenericLayer.identification,								
				sourceDataset.externalIdentification,
				dataSource.identification);
		
		CompletableFuture<Stream<ServiceInfo>> serviceInfoResult = serviceInfoQuery.thenCompose(serviceInfo ->
			serviceDatasetInfoQuery.thenApply(serviceDatasetInfo -> 
				DatabaseUtils.leftJoin(
					serviceInfo.asCollection(),
					serviceDatasetInfo.asCollection(), 
					publishedService.serviceId))).thenApply((Stream<Tuple> serviceInfo) ->
						StreamUtils
							.partition(
								serviceInfo, 
								tuple -> tuple.get(publishedService.serviceId))
							.map(servicePartition -> {
								Tuple serviceInfoFirst = servicePartition.first();
								
								return new ServiceInfo(
									serviceInfoFirst.get(serviceGenericLayer.identification),
									serviceInfoFirst.get(serviceGenericLayer.name),
									serviceInfoFirst.get(service.wmsMetadataFileIdentification),
									serviceInfoFirst.get(service.wfsMetadataFileIdentification),
									StreamUtils
										.partition(									
											servicePartition.stream(),
											tuple -> tuple.get(dataset.id))
										.map(datasetRefPartition -> {
											Tuple datasetRefFirst = datasetRefPartition.first();
											
											return new DatasetRef(										
												datasetRefFirst.get(dataset.identification),
												datasetRefFirst.get(dataset.metadataIdentification),
												datasetRefFirst.get(dataset.metadataFileIdentification),
												datasetRefPartition.stream()
													.map(tuple -> tuple.get(layerGenericLayer.identification))
													.collect(Collectors.toSet()));
										})
										.collect(Collectors.toSet()));
							}));
		
		CompletableFuture<TypedList<Tuple>> datasetInfoQuery = tx.query().from(publishedService)
			.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))
			.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
			.join(service).on(service.id.eq(publishedService.serviceId))
			.join(serviceGenericLayer).on(serviceGenericLayer.id.eq(service.genericLayerId))
			.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
			.join(leafLayer).on(leafLayer.datasetId.eq(dataset.id))
			.join(layerGenericLayer).on(layerGenericLayer.id.eq(leafLayer.genericLayerId))
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.orderBy(dataset.id.asc(), service.id.asc())
			.where(environment.identification.eq(environmentId))
			.list(
				layerGenericLayer.identification,
				publishedService.content,
				serviceGenericLayer.identification,
				serviceGenericLayer.name,
				dataset.identification,
				dataSource.identification,
				sourceDataset.externalIdentification,
				dataset.metadataIdentification,
				dataset.metadataFileIdentification);

		CompletableFuture<Stream<DatasetInfo>> datasetInfoResult = datasetInfoQuery.thenApply(datasetInfo ->
			StreamUtils
				.partition(
					datasetInfo.asCollection().stream(),
					tuple -> tuple.get(dataset.id))
				.map(datasetPartition -> {
					Tuple datasetInfoFirst = datasetPartition.first();
					
					return new DatasetInfo(
						datasetInfoFirst.get(dataset.identification), 
						datasetInfoFirst.get(dataSource.identification), 
						datasetInfoFirst.get(sourceDataset.externalIdentification),
						datasetInfoFirst.get(dataset.metadataIdentification),
						datasetInfoFirst.get(dataset.metadataFileIdentification),
						StreamUtils
							.partition(
								datasetPartition.stream(),
								tuple -> tuple.get(service.id))
							.map(datasetRefPartition -> {
								Tuple datasetRefFirst = datasetRefPartition.first();
								
								Map<String, Set<String>> layerNames = traverseLayers(
									new HashMap<>(), 
									JsonService.fromJson(datasetRefFirst.get(publishedService.content))
										.getLayers());
							
								return new ServiceRef(
									datasetRefFirst.get(serviceGenericLayer.identification),
									datasetRefFirst.get(serviceGenericLayer.name),
									datasetRefPartition.stream()
										.flatMap(tuple -> {
											String layerId = tuple.get(layerGenericLayer.identification);
											
											if(layerNames.containsKey(layerId)) {
												return layerNames.get(layerId).stream();
											} else {
												throw new RuntimeException("layerNames missing for layerId: " + layerId);
											}
										})
										.collect(Collectors.toSet()));
							})
							.collect(Collectors.toSet()));
				}));
			
		return serviceInfoResult.thenCompose(serviceInfo ->
			datasetInfoResult.thenApply(datasetInfo ->
				new MetadataInfo(serviceInfo, datasetInfo)));
	}
	
	public Iterator<DatasetInfo> getDatasets() {
		return datasetInfo.iterator();
	}
	
	public Iterator<ServiceInfo> getServices() {
		return serviceInfo.iterator();
	}

	@Override
	public String toString() {
		return "MetadataInfo []";
	}
}
