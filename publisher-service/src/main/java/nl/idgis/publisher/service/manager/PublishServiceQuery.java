package nl.idgis.publisher.service.manager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.NumberExpression;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;

import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.json.JsonService;
import nl.idgis.publisher.utils.FutureUtils;

import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceStyle.publishedServiceStyle;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QStyle.style;

public class PublishServiceQuery extends AbstractServiceQuery<Ack, SQLSubQuery> {
	
	private final AsyncHelper tx;

	private final Service stagingService;
	
	private final Set<String> environmentIds;

	public PublishServiceQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, Service stagingService, Set<String> environmentIds) {
		super(log, f, new SQLSubQuery());
		
		this.tx = tx;
		this.stagingService = stagingService;
		this.environmentIds = environmentIds;
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
									tx.delete(publishedService)
										.where(getServicePredicate(publishedService.serviceId))
										.execute();
							});
				});
	}

	@Override
	public CompletableFuture<Ack> result() {
		String serviceIdentification = stagingService.getId();
		
		log.debug("publishing service: {}" , serviceIdentification);
		
		new SQLSubQuery();
		
		return 
			deleteExisting().thenCompose(publishedServices -> {
			log.debug("existing published_service records deleted: {}", publishedServices);
			
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
											.execute().thenApply(styles -> {
												log.debug("published service uses {} styles", styles);
												
												return new Ack();
											});
								}));
		});
	}

}
