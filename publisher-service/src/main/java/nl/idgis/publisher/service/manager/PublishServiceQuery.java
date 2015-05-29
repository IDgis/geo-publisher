package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QPublishedServiceStyle.publishedServiceStyle;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.service.manager.QServiceStructure.serviceStructure;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLInsertClause;

import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.json.JsonService;
import nl.idgis.publisher.utils.FutureUtils;

import akka.event.LoggingAdapter;

import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.NumberExpression;

public class PublishServiceQuery extends AbstractServiceQuery<Ack, SQLSubQuery> {
	
	private final AsyncHelper tx;

	private final Service stagingService;
	
	private final Set<String> unfilteredEnvironmentIds;

	public PublishServiceQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, Service stagingService, Set<String> environmentIds) {
		super(log, f, new SQLSubQuery());
		
		this.tx = tx;
		this.stagingService = stagingService;
		this.unfilteredEnvironmentIds = environmentIds;
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
						tx.delete(publishedServiceEnvironment)
							.where(getServicePredicate(publishedServiceEnvironment.serviceId))
							.execute().thenCompose(environments -> {
								log.debug("existing published_service_environment records deleted: {}", environments);
								
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
				});
	}
	
	private CompletableFuture<Set<String>> getEnvironmentIds () {
		if (!stagingService.isConfidential ()) {
			return CompletableFuture.completedFuture (Collections.unmodifiableSet (unfilteredEnvironmentIds));
		}
		
		return tx
			.query ()
			.from (environment)
			.where (environment.confidential.isTrue ())
			.list (environment.identification)
			.thenApply (identifications -> identifications
				.list ()
				.stream ()
				.filter (unfilteredEnvironmentIds::contains)
				.collect (Collectors.toSet ()));
	}

	@Override
	public CompletableFuture<Ack> result() {
		String serviceIdentification = stagingService.getId();
		
		log.debug("publishing service: {}" , serviceIdentification);
				
		return
			getEnvironmentIds ().thenCompose (environmentIds -> {
				return deleteExisting().thenCompose(publishedServices -> {
				log.debug("existing published_service records deleted: {}", publishedServices);
				
				if(environmentIds.isEmpty()) {
					log.debug("no environmentIds given -> not publishing");
					
					return f.successful(new Ack());
				}
				
				return tx.query().from(service)
						.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
						.where(genericLayer.identification.eq(serviceIdentification))
						.singleResult(service.id).thenCompose(serviceId ->
							tx.insert(publishedService)
							.set(publishedService.serviceId, serviceId.orElseThrow(() -> new IllegalArgumentException("service doesn't exists: " + serviceIdentification)))
							.set(publishedService.content, JsonService.toJson(stagingService))
							.execute().thenCompose(publishedService ->
								tx.insert(publishedServiceEnvironment)
									.columns(
										publishedServiceEnvironment.serviceId,
										publishedServiceEnvironment.environmentId)
									.select(new SQLSubQuery().from(environment)
										.where(environment.identification.in(environmentIds))
										.list(
											serviceId.get(),
											environment.id))
									.execute()).thenCompose(environments -> {
										long missingEnvironments = environmentIds.size() - environments;
										if(missingEnvironments > 0) {
											throw new IllegalArgumentException("" + missingEnvironments + " environments don't exist");
										}
										
										log.debug("service published for {} environments", environments);
			
										return
											tx.insert(publishedServiceStyle)
												.columns(
													publishedServiceStyle.serviceId,
													publishedServiceStyle.identification,
													publishedServiceStyle.name,
													publishedServiceStyle.definition)
												.select(new SQLSubQuery().from(style)
													// TODO: get service filter listed below working properly													
													/*.where(withServiceStructure.from(serviceStructure)
														.where(serviceStructure.serviceIdentification.eq(serviceIdentification)
															.and(serviceStructure.styleIdentification.eq(style.identification)))
														.exists())*/
													.list(
														serviceId.get(),
														style.identification,
														style.name,
														style.definition))
												.execute().thenCompose(styles -> {
													log.debug("published service uses {} styles", styles);
													
													return
														QServiceStructure.withServiceStructure(tx.query(), parent, child)
															.from(serviceStructure)
															.where(serviceStructure.serviceIdentification.eq(serviceIdentification)
																.and(serviceStructure.datasetId.isNotNull()))															
															.list(serviceStructure.datasetId).thenCompose(datasetIds -> {
																if(datasetIds.list().isEmpty()) {
																	return f.successful(0l);
																} else {																
																	AsyncSQLInsertClause insert = tx.insert(publishedServiceDataset);
																	
																	for(int datasetId : datasetIds) {
																		log.debug("storing reference to datasetId: " + datasetId);
																		
																		insert
																			.set(publishedServiceDataset.serviceId, serviceId.get()) 
																			.set(publishedServiceDataset.datasetId, datasetId)
																			.addBatch();
																	}
																	
																	return insert.execute();
																}
															}).thenApply(datasets -> {
																log.debug("published service uses {} datasets", datasets);
											
																return new Ack();
															});
												});
									}));
			});
		});
	}

}
