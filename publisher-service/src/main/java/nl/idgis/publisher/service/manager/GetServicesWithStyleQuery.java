package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QStyle.style;

import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.SQLSubQuery;

import akka.event.LoggingAdapter;
import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class GetServicesWithStyleQuery extends AbstractServiceQuery<TypedList<String>> {
	
	private final String styleId;

	GetServicesWithStyleQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String styleId) {
		super(log, f, tx);
		
		this.styleId = styleId;
	}

	@Override
	CompletableFuture<TypedList<String>> result() {
		return withServiceStructure.from(service)
			.where(new SQLSubQuery().from(serviceStructure)
				.join(leafLayer).on(leafLayer.genericLayerId.eq(serviceStructure.childLayerId))
				.join(layerStyle).on(layerStyle.layerId.eq(leafLayer.id))
				.join(style).on(style.id.eq(layerStyle.styleId))
				.join(genericLayer).on(service.genericLayerId.eq(genericLayer.id))
				.where(serviceStructure.serviceIdentification.eq(genericLayer.identification)
					.and(style.identification.eq(styleId)))
				.exists())
			.list(genericLayer.identification);
	}
}
