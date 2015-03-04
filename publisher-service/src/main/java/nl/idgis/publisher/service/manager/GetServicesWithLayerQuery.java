package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QService.service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedIterable;

public class GetServicesWithLayerQuery extends AbstractServiceQuery<TypedIterable<String>> {
	
	private final String layerId;

	GetServicesWithLayerQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String layerId) {
		super(log, f, tx);
		
		this.layerId = layerId;
	}

	@Override
	CompletableFuture<TypedIterable<String>> result() {
		return withServiceStructure.from(serviceStructure)
			.where(serviceStructure.childLayerIdentification.eq(layerId))
			.distinct()
			.list(serviceStructure.serviceIdentification).thenCompose(child ->
				tx.query().from(service)
					.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
					.where(genericLayer.identification.eq(layerId))
					.distinct()						
					.list(service.identification).thenApply(root -> {
						Set<String> serviceIds = new HashSet<>();
						serviceIds.addAll(child.list());
						serviceIds.addAll(root.list());
						
						return new TypedIterable<>(String.class, serviceIds);
					}));
	}
}
