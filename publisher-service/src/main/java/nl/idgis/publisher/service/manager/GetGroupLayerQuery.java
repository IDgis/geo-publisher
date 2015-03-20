package nl.idgis.publisher.service.manager;

import static com.mysema.query.types.PathMetadataFactory.forVariable;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DefaultDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultGroupLayer;
import nl.idgis.publisher.domain.web.tree.DefaultStyleRef;
import nl.idgis.publisher.domain.web.tree.PartialGroupLayer;
import nl.idgis.publisher.domain.web.tree.StyleRef;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

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
	
	private final static QGroupStructure groupStructure = new QGroupStructure("group_structure");	
	
	private static class QGroupStructure extends EntityPathBase<QGroupStructure> {		
		
		private static final long serialVersionUID = -9048925641878000032L;
		
		StringPath groupLayerIdentification = createString("group_layer_identification");

		NumberPath<Integer> parentLayerId = createNumber("parent_layer_id", Integer.class);
		
		StringPath parentLayerIdentification = createString("parent_layer_identification");
		
		NumberPath<Integer> childLayerId = createNumber("child_layer_id", Integer.class);
		
		StringPath childLayerIdentification = createString("child_layer_identification");
		
		NumberPath<Integer> layerOrder = createNumber("layer_order", Integer.class);
		
		StringPath styleIdentification = createString("style_identification");
		
		StringPath styleName = createString("style_name");
		
		QGroupStructure(String variable) {
	        super(QGroupStructure.class, forVariable(variable));
	        
	        add(groupLayerIdentification);
	        add(parentLayerId);
	        add(parentLayerIdentification);
	        add(childLayerId);
	        add(childLayerIdentification);
	        add(layerOrder);	   
	        add(styleIdentification);
	        add(styleName);
	    }
	}
	
	private final FutureUtils f;
		
	private final String groupLayerId;
	
	private final AsyncSQLQuery withGroupStructure;
	
	@SuppressWarnings("unchecked")
	GetGroupLayerQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String groupLayerId) {
		super(log);
		
		this.f = f;
		this.groupLayerId = groupLayerId;
		
		withGroupStructure = tx.query().withRecursive(groupStructure, 
			groupStructure.groupLayerIdentification,
			groupStructure.childLayerId, 
			groupStructure.childLayerIdentification,
			groupStructure.parentLayerId,
			groupStructure.parentLayerIdentification,
			groupStructure.layerOrder,
			groupStructure.styleIdentification,
			groupStructure.styleName).as(
			new SQLSubQuery().unionAll( // TODO: unionAll -> union (doesn't work in H2)
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(genericLayer).on(genericLayer.id.eq(layerStructure.parentLayerId))
					.leftJoin(style).on(style.id.eq(layerStructure.styleId))
					.list(
						genericLayer.identification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder,
						style.identification,
						style.name),
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(groupStructure).on(groupStructure.childLayerId.eq(layerStructure.parentLayerId))
					.leftJoin(style).on(style.id.eq(layerStructure.styleId))
					.list(
						groupStructure.groupLayerIdentification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder,
						style.identification,
						style.name)));
	}
	
	private CompletableFuture<TypedList<Tuple>> structure() {
		return withGroupStructure.clone()
			.from(groupStructure)
			.where(groupStructure.groupLayerIdentification.eq(groupLayerId))
			.orderBy(groupStructure.layerOrder.asc())
			.list(
				groupStructure.styleIdentification,
				groupStructure.styleName,
				groupStructure.childLayerIdentification, 
				groupStructure.parentLayerIdentification);
	}
	
	private CompletableFuture<TypedList<PartialGroupLayer>> groups() {
		return new GroupQuery(log).result();
	}
	
	private CompletableFuture<TypedList<DefaultDatasetLayer>> datasets() {
		return new DatasetQuery(log).result();
	}

	@Override
	CompletableFuture<Object> result() {
		return groups().thenCompose(groups ->
			groups.list().isEmpty() ? f.successful(new NotFound()) : 
				structure().thenCompose(structure ->															
				datasets().thenApply(datasets -> {							
					// LinkedHashMap is used to preserve layer order
					Map<String, String> structureMap = new LinkedHashMap<>();
					
					Map<String, StyleRef> styleMap = new HashMap<>();
					
					for(Tuple structureTuple : structure) {
						String styleId = structureTuple.get(groupStructure.styleIdentification);
						String styleName = structureTuple.get(groupStructure.styleName);
						String childId = structureTuple.get(groupStructure.childLayerIdentification);
						String parentId = structureTuple.get(groupStructure.parentLayerIdentification); 
						
						if(structureMap.containsKey(childId)) {
							throw new IllegalStateException("cycle detected, layer: " + childId);
						}
						
						structureMap.put(childId, parentId);
						if(styleId != null) {
							styleMap.put(childId, new DefaultStyleRef(styleId, styleName));
						}
					}
					
					log.debug("datasets: {}, groups: {}, structure: {}, styles: {}", datasets, groups, structureMap, styleMap);
	
					return DefaultGroupLayer.newInstance(
						groupLayerId,
						datasets.list(),
						groups.list(),
						structureMap,
						styleMap);
				})));
	}

}
