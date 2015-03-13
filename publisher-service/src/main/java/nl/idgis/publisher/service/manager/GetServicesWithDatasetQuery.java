package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QDataset.dataset;

import java.util.concurrent.CompletableFuture;

import com.mysema.query.sql.SQLSubQuery;

import akka.event.LoggingAdapter;
import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class GetServicesWithDatasetQuery extends AbstractServiceQuery<TypedList<String>> {

	private final String datasetId;

	GetServicesWithDatasetQuery (final LoggingAdapter log, final FutureUtils f, final AsyncHelper tx, final String datasetId) {
		super(log, f, tx);
		
		this.datasetId = datasetId;
	}
	
	@Override
	CompletableFuture<TypedList<String>> result() {
		return withServiceStructure.from(service)
				.join(genericLayer).on(service.genericLayerId.eq(genericLayer.id))				
			.where(new SQLSubQuery().from(serviceStructure)
				.join (leafLayer).on (serviceStructure.childLayerId.eq (leafLayer.genericLayerId))
				.join (dataset).on (leafLayer.datasetId.eq (dataset.id))
				.where(genericLayer.identification.eq(serviceStructure.serviceIdentification)
					.and(dataset.identification.eq(datasetId)))
				.exists())			
			.list(genericLayer.identification);
	}
}
