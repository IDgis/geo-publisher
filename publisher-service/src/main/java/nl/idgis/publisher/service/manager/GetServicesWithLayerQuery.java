package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QService.service;

import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.SQLSubQuery;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class GetServicesWithLayerQuery extends AbstractServiceQuery<TypedList<String>, AsyncSQLQuery> {
	
	private final String layerId;

	GetServicesWithLayerQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String layerId) {
		super(log, f, tx.query());
		
		this.layerId = layerId;
	}

	@Override
	CompletableFuture<TypedList<String>> result() {
		return withServiceStructure.from(service)
				.join(genericLayer).on(service.genericLayerId.eq(genericLayer.id))
			.where(new SQLSubQuery().from(serviceStructure)
				.where(serviceStructure.serviceIdentification.eq(genericLayer.identification)
					.and(serviceStructure.childLayerIdentification.eq(layerId)))
				.exists())
			.list(genericLayer.identification);
	}
}
