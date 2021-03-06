package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.service.manager.QServiceStructure.serviceStructure;

import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.SQLSubQuery;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class GetServicesWithStyleQuery extends AbstractServiceQuery<TypedList<String>, AsyncSQLQuery> {
	
	private final String styleId;

	GetServicesWithStyleQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String styleId) {
		super(log, f, tx.query());
		
		this.styleId = styleId;
	}

	@Override
	CompletableFuture<TypedList<String>> result() {
		return withServiceStructure.from(service)
				.join(genericLayer).on(service.genericLayerId.eq(genericLayer.id))
			.where(new SQLSubQuery().from(serviceStructure)
				.join(leafLayer).on(leafLayer.genericLayerId.eq(serviceStructure.childLayerId))
				.join(layerStyle).on(layerStyle.layerId.eq(leafLayer.id))
				.join(style).on(style.id.eq(layerStyle.styleId))				
				.where(serviceStructure.serviceIdentification.eq(genericLayer.identification)
					.and(style.identification.eq(styleId)))
				.exists())
			.list(genericLayer.identification);
	}
}
