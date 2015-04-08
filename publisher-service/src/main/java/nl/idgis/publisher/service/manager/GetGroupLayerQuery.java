package nl.idgis.publisher.service.manager;

import static com.mysema.query.types.PathMetadataFactory.forVariable;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;

import java.util.concurrent.CompletableFuture;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.expr.SimpleExpression;
import com.mysema.query.types.path.BooleanPath;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import akka.event.LoggingAdapter;
import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.AbstractDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultGroupLayer;
import nl.idgis.publisher.domain.web.tree.PartialGroupLayer;
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
		
		StringPath path = createString("path");
		
		BooleanPath cycle = createBoolean("cycle");
		
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
	        add(path);
	        add(cycle);
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
		
		SimpleExpression<String> pathElement = Expressions.template(
			String.class, 
			"'(' || {0} || ',' || {1} || ')'", 
			child.id, 
			parent.id);
		
		withGroupStructure = tx.query().withRecursive(groupStructure, 
			groupStructure.groupLayerIdentification,
			groupStructure.childLayerId, 
			groupStructure.childLayerIdentification,
			groupStructure.parentLayerId,
			groupStructure.parentLayerIdentification,
			groupStructure.layerOrder,
			groupStructure.styleIdentification,
			groupStructure.styleName,
			groupStructure.path,
			groupStructure.cycle).as(
			new SQLSubQuery().unionAll( 
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
						style.name,
						pathElement,
						Expressions.template(Boolean.class, "false")),
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(groupStructure).on(groupStructure.childLayerId.eq(layerStructure.parentLayerId)
						.and(groupStructure.cycle.not()))
					.leftJoin(style).on(style.id.eq(layerStructure.styleId))
					.list(
						groupStructure.groupLayerIdentification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder,
						style.identification,
						style.name,
						groupStructure.path
							.concat(pathElement),
						Expressions.template(
							Boolean.class, 
							"{0} like '%(' || {1} || ',' || {2} || ')%'", 
							groupStructure.path, 
							child.id, 
							parent.id))));
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
	
	private CompletableFuture<TypedList<AbstractDatasetLayer>> datasets() {
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
