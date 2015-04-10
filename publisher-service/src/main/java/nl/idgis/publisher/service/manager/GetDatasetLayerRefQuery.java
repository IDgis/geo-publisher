package nl.idgis.publisher.service.manager;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.AbstractDatasetLayer;

import nl.idgis.publisher.domain.web.tree.DefaultDatasetLayerRef;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;

public class GetDatasetLayerRefQuery extends AbstractQuery<Object> {
	
	private final String layerId;
	
	private final AsyncHelper tx;
	
	public GetDatasetLayerRefQuery(LoggingAdapter log, AsyncHelper tx, String layerId) {
		super(log);
		
		this.tx = tx;
		this.layerId = layerId;
	}
	
	private class DatasetQuery extends AbstractDatasetQuery {

		DatasetQuery(LoggingAdapter log) {
			super(log, tx.query());
		}

		@Override
		protected AsyncSQLQuery filter(AsyncSQLQuery query) {
			return query
				.where(genericLayer.identification.eq(layerId));
		}
		
	}

	@Override
	CompletableFuture<Object> result() {
		return new DatasetQuery(log).result().thenApply(typedList -> {
			List<AbstractDatasetLayer> list = typedList.list();
			
			if(list.isEmpty()) {
				return new NotFound();
			} else {
				Iterator<AbstractDatasetLayer> itr = list.iterator();
				
				AbstractDatasetLayer datasetlayer = itr.next();
				
				if(itr.hasNext()) {
					throw new IllegalStateException("multiple datasets found");
				} 
				
				return new DefaultDatasetLayerRef(datasetlayer, null);
			}
		});			
	}

}
