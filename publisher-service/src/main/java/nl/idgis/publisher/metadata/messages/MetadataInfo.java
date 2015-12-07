package nl.idgis.publisher.metadata.messages;

import static nl.idgis.publisher.database.QDataSource.dataSource;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QPublishedServiceKeyword.publishedServiceKeyword;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mysema.query.Tuple;
import com.mysema.query.types.Expression;
import com.mysema.query.types.QTuple;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.DatabaseUtils;

import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.metadata.MetadataInfoProcessor;
import nl.idgis.publisher.service.json.JsonService;
import nl.idgis.publisher.utils.StreamUtils;

/**
 * Contains all dataset and service information required by {@link MetadataInfoProcessor}. 
 * 
 * @author Reijer Copier <reijer.copier@idgis.nl>
 *
 */
public class MetadataInfo implements Serializable {	

	private static final long serialVersionUID = 140228953110487193L;	
	
	private final Stream<ServiceInfo> serviceInfo;
	
	private final Stream<DatasetInfo> datasetInfo;	

	private MetadataInfo(Stream<ServiceInfo> serviceInfo, Stream<DatasetInfo> datasetInfo) {		
		this.serviceInfo = Objects.requireNonNull(serviceInfo, "serviceInfo must not be null");
		this.datasetInfo = Objects.requireNonNull(datasetInfo, "datasetInfo must not be null");
	}
	
	/**
	 * Fetch (service X keyword)
	 * 
	 * @param tx
	 * @param environmentId
	 * @return
	 */
	protected static CompletableFuture<Stream<Tuple>> fetchServiceKeywords(AsyncHelper tx, String environmentId) {
		return tx.query().from(publishedService)
			.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.leftJoin(publishedServiceKeyword).on(publishedServiceKeyword.serviceId.eq(publishedService.serviceId))
			.where(environment.identification.eq(environmentId))
			.orderBy(publishedService.serviceId.asc())
			.list(
				publishedService.serviceId,
				publishedServiceKeyword.keyword).thenApply(tuples ->
					toServiceKeywords(tuples.asCollection().stream()));
	}
	
	protected final static Expression<Set<String>> serviceKeywords = DatabaseUtils.namedExpression("serviceKeywords");
	
	/**
	 * Collapse (service X keyword)
	 * 
	 * @param stream
	 * @return
	 */
	protected static Stream<Tuple> toServiceKeywords(Stream<Tuple> stream) {
		QTuple keywordTuple = new QTuple(publishedService.serviceId, serviceKeywords);
		
		return StreamUtils
			.partition(
				stream,
				tuple -> tuple.get(publishedService.serviceId))
			.map(servicePartition ->
				keywordTuple.newInstance(
					servicePartition.first().get(publishedService.serviceId),
					servicePartition.stream()
						.map(tuple -> tuple.get(publishedServiceKeyword.keyword))
						.filter(keyword -> keyword != null)
						.collect(Collectors.toSet())));
	}
	
	/**
	 * Fetch (service X dataset X layer)
	 * 
	 * @param tx
	 * @param environmentId
	 * @return
	 */
	protected static CompletableFuture<Stream<Tuple>> fetchServiceDatasetRefs(AsyncHelper tx, String environmentId) {
		return tx.query().from(publishedService)
			.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))
			.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
			.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))			
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.where(environment.identification.eq(environmentId))
			.orderBy(publishedService.serviceId.asc(), dataset.id.asc())
			.list(
				publishedService.serviceId,
				dataset.id,
				dataset.identification,
				dataset.metadataIdentification,
				dataset.metadataFileIdentification,
				publishedServiceDataset.layerName).thenApply(tuples -> 
					toServiceDatasetRef(tuples.asCollection().stream()));
	}
	
	protected final static Expression<Set<DatasetRef>> serviceDatasetRef = DatabaseUtils.namedExpression("serviceDatasetRef");
	
	/**
	 * Collapse (service X dataset X layer) into {@link DataRef} instances.
	 * 
	 * @param stream
	 * @return
	 */
	protected static Stream<Tuple> toServiceDatasetRef(Stream<Tuple> stream) {
		QTuple datasetRefTuple = new QTuple(publishedService.serviceId, serviceDatasetRef);
		
		return 
			StreamUtils
				.partition(
					stream, 
					tuple -> tuple.get(publishedService.serviceId))
				.map(servicePartition ->			
					datasetRefTuple.newInstance(
						servicePartition.first().get(publishedService.serviceId),
						StreamUtils
							.partition(
								servicePartition.stream(),
								tuple -> tuple.get(dataset.id))
							.map(datasetPartition -> {
								Tuple datasetTuple = datasetPartition.first();
								
								return new DatasetRef(
									datasetTuple.get(dataset.identification),
									datasetTuple.get(dataset.metadataIdentification),
									datasetTuple.get(dataset.metadataFileIdentification),
									datasetPartition.stream()
										.map(tuple -> tuple.get(publishedServiceDataset.layerName))
										.collect(Collectors.toSet()));
							})
							.collect(Collectors.toSet())));
	}
	
	protected static CompletableFuture<Stream<Tuple>> fetchServiceGeneralInfo(AsyncHelper tx, String environmentId) {
		return tx.query().from(publishedService)
			.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))
			.join(service).on(service.id.eq(publishedService.serviceId))			
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.where(environment.identification.eq(environmentId))
			.orderBy(publishedService.serviceId.asc())
			.list(
				publishedService.title,
				publishedService.alternateTitle,
				publishedService.abstractCol,
				publishedService.serviceId,
				publishedService.content,
				service.wfsMetadataFileIdentification,
				service.wmsMetadataFileIdentification).thenApply(tuples -> 
					tuples.asCollection().stream());
	}
	
	protected static CompletableFuture<Stream<ServiceInfo>> fetchServiceInfo(AsyncHelper tx, String environmentId) {
		return
			fetchServiceGeneralInfo(tx, environmentId).thenCompose(serviceGeneralInfo ->
			fetchServiceKeywords(tx, environmentId).thenCompose(serviceKeywordInfo ->
			fetchServiceDatasetRefs(tx, environmentId).thenApply(serviceDatasetRefInfo ->
				DatabaseUtils.join(
					DatabaseUtils.join(
						serviceGeneralInfo,
						serviceDatasetRefInfo, 
						publishedService.serviceId),
					serviceKeywordInfo,
					publishedService.serviceId)
						.map(tuple -> {
							Service serviceContent = JsonService.fromJson(
								tuple.get(publishedService.content), 
								Collections.emptyMap() /* datasetIds, empty because we don't use layer info */);
							
							return new ServiceInfo(
								serviceContent.getId(),
								serviceContent.getName(),
								tuple.get(publishedService.title),
								tuple.get(publishedService.alternateTitle),
								tuple.get(publishedService.abstractCol),
								tuple.get(service.wmsMetadataFileIdentification),
								tuple.get(service.wfsMetadataFileIdentification),
								tuple.get(serviceDatasetRef),
								tuple.get(serviceKeywords),
								new ContactInfo(
									serviceContent.getContact(),
									serviceContent.getOrganization(),
									serviceContent.getPosition(),
									serviceContent.getAddressType(),
									serviceContent.getAddress(),
									serviceContent.getCity(),
									serviceContent.getState(),
									serviceContent.getZipcode(),
									serviceContent.getCountry(),
									serviceContent.getTelephone(),
									serviceContent.getFax(),
									serviceContent.getEmail()));
						}))));
	}
	
	/**
	 * Fetch (service X dataset X layer)
	 * 
	 * @param tx
	 * @param environmentId
	 * @return
	 */
	protected static CompletableFuture<Stream<Tuple>> fetchDatasetServiceRef(AsyncHelper tx, String environmentId) {
		return tx.query().from(publishedService)			
			.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))
			.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
			.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))			
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.where(environment.identification.eq(environmentId))
			.orderBy(dataset.id.asc(), publishedService.serviceId.asc())
			.list(
				dataset.id,
				publishedService.serviceId,
				publishedService.content,
				publishedServiceDataset.layerName).thenApply(tuples ->
					toDatasetServiceRef(tuples.asCollection().stream()));
	}
	
	protected final static Expression<Set<ServiceRef>> datasetServiceRef = DatabaseUtils.namedExpression("datasetServiceRef");
	
	/**
	 * Collapse (service X dataset X layer) into ServiceRef
	 * 
	 * @param stream
	 * @return
	 */
	protected static Stream<Tuple> toDatasetServiceRef(Stream<Tuple> stream) {
		QTuple serviceRefTuple = new QTuple(dataset.id, datasetServiceRef);
		
		return StreamUtils
			.partition(
				stream,
				tuple -> tuple.get(dataset.id))
			.map(datasetPartition ->
				serviceRefTuple.newInstance(
					datasetPartition.first().get(dataset.id),
					StreamUtils
						.partition(
							datasetPartition.stream(),
							tuple -> tuple.get(publishedService.serviceId))
						.map(servicePartition -> {
							Service serviceContent = JsonService.fromJson(
								servicePartition.first().get(publishedService.content), 
								Collections.emptyMap() /* datasetIds, empty because we don't use layer info */);
							
							return new ServiceRef(
								serviceContent.getId(),
								serviceContent.getName(),
								servicePartition.stream()
									.map(tuple -> tuple.get(publishedServiceDataset.layerName))
									.collect(Collectors.toSet()));
						})
						.collect(Collectors.toSet())));
	}
	
	protected static CompletableFuture<Stream<Tuple>> fetchDatasetGeneralInfo(AsyncHelper tx, String environmentId) {
		return tx.query().from(publishedService)
			.join(publishedServiceEnvironment).on(publishedServiceEnvironment.serviceId.eq(publishedService.serviceId))
			.join(publishedServiceDataset).on(publishedServiceDataset.serviceId.eq(publishedService.serviceId))
			.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))			
			.join(sourceDataset).on(sourceDataset.id.eq(dataset.sourceDatasetId))
			.join(dataSource).on(dataSource.id.eq(sourceDataset.dataSourceId))
			.join(environment).on(environment.id.eq(publishedServiceEnvironment.environmentId))
			.orderBy(dataset.id.asc())
			.where(environment.identification.eq(environmentId))
			.list(
				dataset.id,
				publishedService.content,				
				dataset.identification,
				dataSource.identification,
				sourceDataset.externalIdentification,
				dataset.metadataIdentification,
				dataset.metadataFileIdentification).thenApply(tuples ->
					tuples.asCollection().stream());
	}
	
	protected static CompletableFuture<Stream<DatasetInfo>> fetchDatasetInfo(AsyncHelper tx, String environmentId) {
		return
			fetchDatasetGeneralInfo(tx, environmentId).thenCompose(datasetGeneralInfo ->
			fetchDatasetServiceRef(tx, environmentId).thenApply(datasetServiceRefInfo ->
				DatabaseUtils
					.join(
						datasetGeneralInfo,
						datasetServiceRefInfo,
						dataset.id)
					.map(tuple ->
						new DatasetInfo(
							tuple.get(dataset.identification), 
							tuple.get(dataSource.identification), 
							tuple.get(sourceDataset.externalIdentification),
							tuple.get(dataset.metadataIdentification),
							tuple.get(dataset.metadataFileIdentification),
							tuple.get(datasetServiceRef)))));
	}
	
	public static CompletableFuture<MetadataInfo> fetch(AsyncHelper tx, String environmentId) {	
		return 
			fetchServiceInfo(tx, environmentId).thenCompose(serviceInfo ->
			fetchDatasetInfo(tx, environmentId).thenApply(datasetInfo ->
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
