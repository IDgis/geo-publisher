package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceKeyword.publishedServiceKeyword;
import static nl.idgis.publisher.database.QPublishedServiceStyle.publishedServiceStyle;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QServiceKeyword.serviceKeyword;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.service.manager.QServiceStructure.serviceStructure;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLInsertClause;

import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.json.JsonService;
import nl.idgis.publisher.utils.FutureUtils;

import akka.event.LoggingAdapter;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.NumberExpression;

public class PublishServiceQuery extends AbstractServiceQuery<Ack, SQLSubQuery> {
	
	private final AsyncHelper tx;

	private final Service stagingService;
	
	private final String environmentId;

	public PublishServiceQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, Service stagingService, Optional<String> environmentId) {
		super(log, f, new SQLSubQuery());
		
		this.tx = tx;
		this.stagingService = stagingService;
		this.environmentId = environmentId.orElse(null);
	}
	
	private Predicate getServicePredicate(NumberExpression<Integer> idExpr) {
		return new SQLSubQuery().from(service)					
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(stagingService.getId())
				.and(service.id.eq(idExpr)))
			.exists();
	}
	
	private CompletableFuture<Long> deleteExisting() {
		return
			tx.delete(publishedServiceStyle)
				.where(getServicePredicate(publishedServiceStyle.serviceId))
				.execute().thenCompose(styles -> {
					log.debug("existing published_service_style records deleted: {}", styles);
					
					return
							tx.delete(publishedServiceDataset)
								.where(getServicePredicate(publishedServiceDataset.serviceId))
								.execute().thenCompose(datasets -> {
									log.debug("existing published_service_dataset records deleted: {}", datasets);
						
									return 
										tx.delete(publishedService)
											.where(getServicePredicate(publishedService.serviceId))
											.execute();
						});
				});
	}
	
	private CompletableFuture<Optional<Integer>> getEnvironmentId() {
		if(environmentId == null) {
			return f.successful(Optional.empty());
		}
		
		return  
			tx.query().from(environment)
			.where(environment.identification.eq(environmentId))
			.singleResult(environment.confidential, environment.wmsOnly, environment.id).thenApply(optionalEnvironmentInfo -> {
				Tuple environmentInfo = optionalEnvironmentInfo.orElseThrow(() -> 
					new IllegalArgumentException("environment doesn't exist: " + environmentId));
				
				if(!environmentInfo.get(environment.confidential) 
						&& !environmentInfo.get(environment.wmsOnly) && stagingService.isWmsOnly()) {
					throw new IllegalArgumentException("wmsonly error: environment is not confidential or "
							+ "wms only: " + environmentId);
				}
				
				if(!environmentInfo.get(environment.confidential) && stagingService.isConfidential() 
						&& (!environmentInfo.get(environment.wmsOnly) || !stagingService.isWmsOnly())) {
					throw new IllegalArgumentException("confidential error: environment is not "
							+ "confidential or wms only: " + environmentId);
				}
				
				return Optional.of(environmentInfo.get(environment.id));
			});
	}

	@Override
	public CompletableFuture<Ack> result() {
		String serviceIdentification = stagingService.getId();
		
		log.debug("publishing service: {}" , serviceIdentification);
				
		return
			getEnvironmentId().thenCompose (environmentId ->
			deleteExisting().thenCompose(publishedServices -> {
				log.debug("existing published_service records deleted: {}", publishedServices);
				
				if(environmentId.isPresent()) {
					return getServiceInfo(serviceIdentification).thenCompose(serviceInfo ->
						insert(
							serviceIdentification, 
							serviceInfo.orElseThrow(() -> 
								new IllegalArgumentException("service doesn't exist: " + serviceIdentification)),
							environmentId.get()));
				} else {
					log.debug("no environmentId given -> not publishing");
					
					return f.successful(new Ack());
				}
		}));
	}

	private CompletionStage<Ack> insert(String serviceIdentification, Tuple serviceInfo, int environmentId) {
		int serviceId = serviceInfo.get(service.id);
		
		return 
			insertPublishedService(serviceInfo, serviceId, environmentId).thenCompose(publishedService ->
			insertPublishedServiceKeyword(serviceId).thenCompose(publishedServiceKeywords ->
			insertPublishedServiceStyle(serviceIdentification, serviceId).thenCompose(publishedServiceStyles ->
			insertPublishedServiceDataset(serviceIdentification, serviceId).thenApply(publishedServiceDatasets -> {
				
				log.debug("published service has {} keywords", publishedServiceKeywords);
				log.debug("published service uses {} styles", publishedServiceStyles);
				log.debug("published service uses {} datasets", publishedServiceDatasets);
			
				return new Ack();
			}))));
	}

	private CompletableFuture<Long> insertPublishedServiceDataset(String serviceIdentification, int serviceId) {
		return QServiceStructure.withServiceStructure(tx.query(), parent, child)
			.from(serviceStructure)			
			.where(serviceStructure.serviceIdentification.eq(serviceIdentification)
				.and(serviceStructure.datasetId.isNotNull()))
			.distinct()
			.list(serviceStructure.datasetId, serviceStructure.layerName).thenCompose(tuples -> {
				if(tuples.list().isEmpty()) {
					return f.successful(0l);
				} else {																
					AsyncSQLInsertClause publishedServiceDatasetInsert = tx.insert(publishedServiceDataset);
					
					for(Tuple tuple : tuples) {
						int datasetId = tuple.get(serviceStructure.datasetId);
						String layerName = tuple.get(serviceStructure.layerName);
						
						log.debug("storing reference to datasetId: {}, layerName: {}", datasetId, layerName);
						
						publishedServiceDatasetInsert
							.set(publishedServiceDataset.serviceId, serviceId) 
							.set(publishedServiceDataset.datasetId, datasetId)
							.set(publishedServiceDataset.layerName, layerName)
							.addBatch();
					}
					
					return publishedServiceDatasetInsert.execute();
				}
			});
	}

	private CompletableFuture<Long> insertPublishedServiceStyle(String serviceIdentification, int serviceId) {
		return QServiceStructure.withServiceStructure(tx.query(), parent, child)
			.from(serviceStructure)
			.join(layerStyle).on(layerStyle.layerId.eq(serviceStructure.leafLayerId))
			.join(style).on(style.id.eq(layerStyle.styleId))
			.where(serviceStructure.serviceIdentification.eq(serviceIdentification))
			.groupBy(
				style.identification,
				style.name,
				style.definition)
			.list(
				style.identification,
				style.name,
				style.definition).thenCompose(styles -> {
				
				if(styles.list().isEmpty()) {
					return f.successful(0l);
				} else {
					AsyncSQLInsertClause publishedServiceStyleInsert = tx.insert(publishedServiceStyle);
					
					for(Tuple currentStyle : styles) {														
						String styleIdentification = currentStyle.get(style.identification);
						String styleName = currentStyle.get(style.name);
						String styleDefinition = currentStyle.get(style.definition);
						
						log.debug("storing style: {}", styleName);
						
						publishedServiceStyleInsert
							.set(publishedServiceStyle.serviceId, serviceId)
							.set(publishedServiceStyle.identification, styleIdentification)
							.set(publishedServiceStyle.name, styleName)
							.set(publishedServiceStyle.definition, styleDefinition)
							.addBatch();
					}
					
					return publishedServiceStyleInsert.execute();
				}
			});
	}	

	private CompletableFuture<Long> insertPublishedService(Tuple tuple, int serviceId, int environmentId) {
		return tx.insert(publishedService)
			.set(publishedService.serviceId, serviceId)
			.set(publishedService.environmentId, environmentId)
			.set(publishedService.title, tuple.get(genericLayer.title))
			.set(publishedService.alternateTitle, tuple.get(service.alternateTitle))
			.set(publishedService.abstractCol, tuple.get(genericLayer.abstractCol))
			.set(publishedService.content, JsonService.toJson(stagingService))
			.execute();
	}

	private CompletableFuture<Optional<Tuple>> getServiceInfo(String serviceIdentification) {
		return tx.query().from(service)
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.where(genericLayer.identification.eq(serviceIdentification))
				.singleResult(
					service.id,
					genericLayer.title,
					service.alternateTitle,
					genericLayer.abstractCol);
	}

	private CompletableFuture<Long> insertPublishedServiceKeyword(int serviceId) {
		return tx.insert(publishedServiceKeyword)
			.columns(
				publishedServiceKeyword.serviceId,
				publishedServiceKeyword.keyword)
			.select(new SQLSubQuery().from(serviceKeyword)
				.where(serviceKeyword.serviceId.eq(serviceId))
				.list(
					serviceId,
					serviceKeyword.keyword))
			.execute();
	}

}
