package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.service.manager.QGroupStructure.groupStructure;

import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DefaultDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultGroupLayer;
import nl.idgis.publisher.domain.web.tree.PartialGroupLayer;
import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;
import akka.event.LoggingAdapter;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;

public class GetGroupLayerQuery extends AbstractQuery<Object> {
	
	private class GroupQuery extends AbstractGroupQuery {
		
		GroupQuery(LoggingAdapter log) {
			super(log);
		}
		
		@Override
		protected AsyncSQLQuery groups() {
			return withGroupStructure.clone()
				.from(genericLayer)				
				.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))
				.where(new SQLSubQuery().from(leafLayer)
					.where(leafLayer.genericLayerId.eq(genericLayer.id))
					.notExists())
				.where(new SQLSubQuery().from(groupStructure) 
					.where(groupStructure.childLayerId.eq(genericLayer.id))
					.where(groupStructure.groupLayerIdentification.eq(groupLayerId)) 
					.exists() // requested group children
						.or(genericLayer.identification.eq(groupLayerId))); // requested group itself
		}
		
	}
	
	private class DatasetQuery extends AbstractDatasetQuery {
		
		DatasetQuery(LoggingAdapter log) {
			super(log, withGroupStructure);
		}
		
		@Override
		protected AsyncSQLQuery filter(AsyncSQLQuery filter) {
			return
				filter
					.join(groupStructure).on(groupStructure.childLayerId.eq(genericLayer.id))
					.where(groupStructure.groupLayerIdentification.eq(groupLayerId));
		}	
	}
	
	private final FutureUtils f;
		
	private final String groupLayerId;
	
	private final AsyncSQLQuery withGroupStructure;
	
	GetGroupLayerQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String groupLayerId) {
		super(log);
		
		this.f = f;
		this.groupLayerId = groupLayerId;
		
		withGroupStructure = QGroupStructure.withGroupStructure (tx.query (), parent, child);
	}
	
	private CompletableFuture<TypedList<Tuple>> structure() {
		return withGroupStructure.clone()
			.from(groupStructure)
			.where(groupStructure.groupLayerIdentification.eq(groupLayerId))
			// order by parentLayerId is required in order to be able eliminate duplicates
			.orderBy(groupStructure.parentLayerId.asc(), groupStructure.layerOrder.asc())
			.list(
				groupStructure.styleIdentification,
				groupStructure.styleName,
				groupStructure.childLayerIdentification, 
				groupStructure.parentLayerIdentification,
				groupStructure.layerOrder,
				groupStructure.cycle);
	}
	
	private CompletableFuture<TypedList<PartialGroupLayer>> groups() {
		return new GroupQuery(log).result();
	}
	
	private CompletableFuture<TypedList<DefaultDatasetLayer>> datasets() {
		return new DatasetQuery(log).result();
	}

	@Override
	CompletableFuture<Object> result() {
		StructureProcessor structureProcessor = new StructureProcessor(
			groupStructure.styleIdentification,
			groupStructure.styleName,
			groupStructure.childLayerIdentification,
			groupStructure.parentLayerIdentification,
			groupStructure.layerOrder,
			groupStructure.cycle);
		
		return groups().thenCompose(groups ->
			groups.list().isEmpty() ? f.successful(new NotFound()) : 
				structure().thenCompose(structure ->															
				datasets().thenCompose(datasets -> {					
						try {
							StructureProcessor.Result transformedStructure 
								= structureProcessor.transform(structure.list());
							
							return f.successful(DefaultGroupLayer.newInstance(
								groupLayerId,
								datasets.list(),
								groups.list(),
								transformedStructure.getStructureItems(),
								transformedStructure.getStyles()));
						} catch (CycleException e) {
							log.debug("CycleException: " + e + "[" +e.getMessage()+"]");
							return f.failed(e);
						}
				})));
	}

}
