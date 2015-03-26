package nl.idgis.publisher.service.manager;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.SQLSubQuery;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;

import nl.idgis.publisher.domain.web.tree.Service;

import nl.idgis.publisher.protocol.messages.Ack;
import nl.idgis.publisher.service.json.JsonService;

import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QEnvironment.environment;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;

public class PublishServiceQuery extends AbstractQuery<Ack> {
		
	private final AsyncHelper tx;
	
	private final Service stagingService;
	
	private final Set<String> environmentIds;

	public PublishServiceQuery(LoggingAdapter log, AsyncHelper tx, Service stagingService, Set<String> environmentIds) {
		super(log);
		
		this.tx = tx;
		this.stagingService = stagingService;
		this.environmentIds = environmentIds;
	}
	
	private CompletableFuture<Long> deleteExisting() {
		return
			tx.delete(publishedServiceEnvironment)
				.where(new SQLSubQuery().from(service)					
					.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
					.where(genericLayer.identification.eq(stagingService.getId())
						.and(service.id.eq(publishedServiceEnvironment.serviceId)))
					.exists())
				.execute().thenCompose(environments -> {
					log.debug("existing published_service_environment records deleted: {}", environments);
					
					return 
						tx.delete(publishedService)
							.where(new SQLSubQuery().from(service)
								.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
								.where(genericLayer.identification.eq(stagingService.getId())
									.and(service.id.eq(publishedService.serviceId)))
								.exists())									
							.execute();
				});
	}

	@Override
	public CompletableFuture<Ack> result() {
		String serviceIdentification = stagingService.getId();
		
		log.debug("publishing service: {}" , serviceIdentification);
		
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
								.execute()).thenApply(environments -> {
									long missingEnvironments = environmentIds.size() - environments;
									if(missingEnvironments > 0) {
										throw new IllegalArgumentException("" + missingEnvironments + " environments don't exist");
									}
									
									log.debug("service published for {} environments", environments);
		
									return new Ack();
								}));
		});
	}

}
