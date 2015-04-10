package nl.idgis.publisher.service.manager;

import java.util.concurrent.CompletableFuture;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.service.json.JsonService;
import nl.idgis.publisher.utils.FutureUtils;

import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QPublishedServiceEnvironment.publishedServiceEnvironment;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QEnvironment.environment;

import static java.util.stream.Collectors.toSet;

public class GetPublishedServiceQuery extends AbstractQuery<Object> {
	
	private final AsyncHelper tx;
	
	private final FutureUtils f;
	
	private final String serviceId;
	
	public GetPublishedServiceQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String serviceId) {
		super(log);
		
		this.f = f;
		this.tx = tx;
		this.serviceId = serviceId;
	}

	@Override
	public CompletableFuture<Object> result() {
		log.debug("fetching published service: {}", serviceId);
		
		return tx.query().from(publishedService)
			.join(service).on(service.id.eq(publishedService.serviceId))
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(publishedService.content).thenApply(optionalServiceContent ->				
				optionalServiceContent
					.<Object>map(JsonService::fromJson)
					.orElse(new NotFound()));
	}

}
