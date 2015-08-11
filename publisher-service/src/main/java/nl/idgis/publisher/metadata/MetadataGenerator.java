package nl.idgis.publisher.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mysema.query.Tuple;

import com.typesafe.config.Config;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncDatabaseHelper;
import nl.idgis.publisher.database.QGenericLayer;

import nl.idgis.publisher.metadata.messages.GenerateMetadata;
import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.json.JsonService;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.StreamUtils;
import nl.idgis.publisher.utils.TypedList;
import nl.idgis.publisher.xml.exceptions.NotFound;

import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.Layer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.harvester.messages.GetDataSource;
import nl.idgis.publisher.harvester.sources.messages.GetDatasetMetadata;

import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;

public class MetadataGenerator extends UntypedActor {
	
	private static final QGenericLayer layerGenericLayer = new QGenericLayer("layerGenericLayer");

	private static final QGenericLayer serviceGenericLayer = new QGenericLayer("serviceGenericLayer");
	
	private static final String ENDPOINT_CODE_LIST_VALUE = "WebServices";

	private static final String ENDPOINT_CODE_LIST = "http://www.isotc211.org/2005/iso19119/resources/Codelist/gmxCodelists.xml#DCPList";

	private static final String ENDPOINT_OPERATION_NAME = "GetCapabilitities";

	private final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
		
	private final ActorRef database, serviceManager, harvester;
	
	private final MetadataStore serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget;
	
	private final Config constants;
	
	private FutureUtils f;
	
	private AsyncDatabaseHelper db;
	
	public MetadataGenerator(ActorRef database, ActorRef serviceManager, ActorRef harvester, MetadataStore serviceMetadataSource, MetadataStore datasetMetadataTarget, MetadataStore serviceMetadataTarget, Config constants) {
		this.database = database;		
		this.serviceManager = serviceManager;
		this.harvester = harvester;
		this.serviceMetadataSource = serviceMetadataSource;
		this.datasetMetadataTarget = datasetMetadataTarget;
		this.serviceMetadataTarget = serviceMetadataTarget;
		this.constants = constants;	
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager, ActorRef harvester, MetadataStore serviceMetadataSource, MetadataStore datasetMetadataTarget, MetadataStore serviceMetadataTarget, Config constants) {
		return Props.create(MetadataGenerator.class, database, serviceManager, harvester, serviceMetadataSource, datasetMetadataTarget, serviceMetadataTarget, constants);
	}
	
	@Override
	public void preStart() throws Exception {		
		f = new FutureUtils(getContext());
		db = new AsyncDatabaseHelper(database, f, log);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if(msg instanceof GenerateMetadata) {
			generateMetadata();
		} else {
			unhandled(msg);
		}
	}
	
	private static void traverseLayers(Map<String, Set<String>> layerInfo, List<LayerRef<? extends Layer>> layerRefs) {
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
	}
	
	private static Map<String, Set<String>> getLayerInfo(Service service) {
		Map<String, Set<String>> layerInfo = new HashMap<>();
		traverseLayers(layerInfo, service.getLayers());
		
		return layerInfo;
	}
	
	private void generateMetadata() throws InterruptedException, ExecutionException, NotFound {		
		
		log.info("generating metadata");
		
		final ActorRef sender = getSender();
		final ActorRef self = getSelf();

		db.transactional(tx ->
			tx.query().from(publishedServiceDataset)
				.join(service).on(service.id.eq(publishedServiceDataset.serviceId))
				.join(serviceGenericLayer).on(serviceGenericLayer.id.eq(service.genericLayerId))
				.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
				.join(leafLayer).on(leafLayer.datasetId.eq(dataset.id))
				.join(layerGenericLayer).on(layerGenericLayer.id.eq(leafLayer.genericLayerId))
				.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
				.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
				.list(
					serviceGenericLayer.identification,
					layerGenericLayer.identification,
					dataset.identification,
					sourceDataset.externalIdentification,
					dataSource.identification)
	
				.thenCompose(joinTuples -> {
					if(log.isDebugEnabled()) {
						log.debug("joinTuples: {}", joinTuples.list().size());
						joinTuples.list().stream()
							.forEach(tuple -> log.debug("joinTuple: {}", tuple));
					}
					
					return tx.query().from(publishedService)			
						.list(publishedService.content).thenApply(tuples ->
							tuples.list().stream()
								.map(JsonService::fromJson)
								.collect(Collectors.toMap(
									service -> service.getId(),
									MetadataGenerator::getLayerInfo)))
						
						.thenCompose(serviceInfo -> {							
							log.debug("serviceInfo: {}", serviceInfo.size());							
							
							return getDatasetMetadata(joinTuples).thenCompose(datasetMetadata -> {
								log.debug("datasetMetadata: {}", datasetMetadata.size());
								
								return getServiceMetadata(joinTuples).thenApply(serviceMetadata -> {
									log.debug("serviceMetadata: {}", serviceMetadata.size());
									
									Map<String, Set<String>> datasetServiceInfo = 
										joinTuples.list().stream()
											.collect(Collectors.groupingBy(
												tuple -> tuple.get(dataset.identification),
												Collectors.mapping(
													tuple -> tuple.get(serviceGenericLayer.identification),
													Collectors.toSet())));
									
									log.debug("datasets: {}", datasetServiceInfo.size());
									for(Map.Entry<String, Set<String>> dataset : datasetServiceInfo.entrySet()) {
										log.debug("dataset {}: {}", dataset.getKey(), dataset.getValue());
									}
									
									Map<String, Set<String>> serviceDatasetInfo = 
										joinTuples.list().stream()
											.collect(Collectors.groupingBy(
												tuple -> tuple.get(serviceGenericLayer.identification),
												Collectors.mapping(
													tuple -> tuple.get(dataset.identification),
													Collectors.toSet())));
									
									log.debug("services: {}", serviceDatasetInfo.size());
									for(Map.Entry<String, Set<String>> service : serviceDatasetInfo.entrySet()) {
										log.debug("service {}: {}", service.getKey(), service.getValue());
									}
									
									log.debug("metadata generation finished");
									
									return new Ack();
								});
							});
						});
					})
		).thenAccept(resp -> sender.tell(resp, self));
	}
	
	private CompletableFuture<Map<String, Optional<MetadataDocument>>> getServiceMetadata(TypedList<Tuple> joinTuples) {
		List<String> serviceIds =
			joinTuples.list().stream()
				.map(tuple -> tuple.get(serviceGenericLayer.identification))
				.distinct()
				.collect(Collectors.toList());
		
		CompletableFuture<Stream<Optional<MetadataDocument>>> metadataDocumentFutures =
			serviceIds.stream()
				.map(serviceId -> serviceMetadataSource.get(serviceId)
					.thenApply(Optional::of)
					.exceptionally(t -> Optional.empty()))
				.collect(f.collect());
		
		return metadataDocumentFutures.thenApply(metadataDocuments ->
			StreamUtils.zipToMap(serviceIds.stream(), metadataDocuments));
	}

	private CompletableFuture<Map<String, Optional<MetadataDocument>>> getDatasetMetadata(TypedList<Tuple> joinTuples) {
		CompletableFuture<Stream<Optional<MetadataDocument>>> metadataDocumentFutures = 
			getDataSources(joinTuples).thenCompose(dataSources ->
				joinTuples.list().stream()
					.map(tuple -> 
						dataSources.get(tuple.get(dataSource.identification))
							.map(dataSource ->
								f.ask(
									dataSource,
									new GetDatasetMetadata(tuple.get(sourceDataset.externalIdentification)),
									MetadataDocument.class).thenApply(Optional::of))
							.orElse(f.successful(Optional.empty())))
					.collect(f.collect()));
		
		Stream<String> datasetIds = 
			joinTuples.list().stream()
				.map(tuple -> tuple.get(dataset.identification));
		
		return metadataDocumentFutures.thenApply(metadataDocuments ->
			StreamUtils.zipToMap(datasetIds, metadataDocuments));
	}

	private CompletableFuture<Map<String, Optional<ActorRef>>> getDataSources(TypedList<Tuple> joinTuples) {
		List<String> dataSourceIds = 
			joinTuples.list().stream()
				.map(tuple -> tuple.get(dataSource.identification))
				.distinct()
				.collect(Collectors.toList());
		
		CompletableFuture<Stream<Object>> harvesterResponseFutures =
				dataSourceIds.stream()			
					.map(dataSourceId -> f.ask(harvester, new GetDataSource(dataSourceId)))
					.collect(f.collect());
		
		CompletableFuture<Stream<Optional<ActorRef>>> dataSourcesFuture =
			harvesterResponseFutures.thenApply(harvesterResponses ->
				harvesterResponses.map(harvesterResponse ->
						harvesterResponse instanceof ActorRef
							? Optional.of((ActorRef)harvesterResponse)
							: Optional.empty()));
		
		return dataSourcesFuture.thenApply(dataSources ->
			StreamUtils.zipToMap(dataSourceIds.stream(), dataSources));
	}
	
}
