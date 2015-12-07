package nl.idgis.publisher.service.manager;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.service.json.JsonService;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QPublishedServiceDataset.publishedServiceDataset;
import static nl.idgis.publisher.database.QPublishedService.publishedService;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;

public class GetPublishedServiceQuery extends AbstractQuery<Object> {
	
	private final AsyncHelper tx;
		
	private final String serviceId;
	
	public GetPublishedServiceQuery(LoggingAdapter log, AsyncHelper tx, String serviceId) {
		super(log);
		
		this.tx = tx;
		this.serviceId = serviceId;
	}

	@Override
	public CompletableFuture<Object> result() {
		log.debug("fetching published service: {}", serviceId);
		
		return tx.query().from(publishedServiceDataset)
			.join(dataset).on(dataset.id.eq(publishedServiceDataset.datasetId))
			.join(service).on(service.id.eq(publishedServiceDataset.serviceId))
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.list(publishedServiceDataset.layerName, dataset.identification)
			.thenCompose(datasetIds ->
				tx.query().from(publishedService)
					.join(service).on(service.id.eq(publishedService.serviceId))
					.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
					.where(genericLayer.identification.eq(serviceId))
					.singleResult(publishedService.content).thenApply(optionalServiceContent ->				
						optionalServiceContent
							.<Object>map(json -> JsonService.fromJson(json, datasetIds.list().stream()
								.collect(Collectors.toMap(
									t -> t.get(publishedServiceDataset.layerName),
									t -> t.get(dataset.identification)))))
							.orElse(new NotFound())));
	}

}
