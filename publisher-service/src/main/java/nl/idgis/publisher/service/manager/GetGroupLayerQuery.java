package nl.idgis.publisher.service.manager;

import static com.mysema.query.types.PathMetadataFactory.forVariable;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QTiledLayerMimeformat.tiledLayerMimeformat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.StringPath;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DefaultDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultGroupLayer;
import nl.idgis.publisher.domain.web.tree.PartialGroupLayer;
import nl.idgis.publisher.domain.web.tree.DefaultTiling;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class GetGroupLayerQuery extends AbstractQuery<Object> {
	
	private final static QGroupStructure groupStructure = new QGroupStructure("group_structure");	
	
	private static class QGroupStructure extends EntityPathBase<QGroupStructure> {		
		
		private static final long serialVersionUID = -9048925641878000032L;
		
		StringPath groupLayerIdentification = createString("group_layer_identification");

		NumberPath<Integer> parentLayerId = createNumber("parent_layer_id", Integer.class);
		
		StringPath parentLayerIdentification = createString("parent_layer_identification");
		
		NumberPath<Integer> childLayerId = createNumber("child_layer_id", Integer.class);
		
		StringPath childLayerIdentification = createString("child_layer_identification");
		
		NumberPath<Integer> layerOrder = createNumber("layer_order", Integer.class);
		
		QGroupStructure(String variable) {
	        super(QGroupStructure.class, forVariable(variable));
	        
	        add(groupLayerIdentification);
	        add(parentLayerId);
	        add(parentLayerIdentification);
	        add(childLayerId);
	        add(childLayerIdentification);
	        add(layerOrder);	        
	    }
	}
	
	private final FutureUtils f;
		
	private final String groupLayerId;
	
	private final AsyncSQLQuery withGroupStructure;
	
	@SuppressWarnings("unchecked")
	GetGroupLayerQuery(FutureUtils f, AsyncHelper tx, String groupLayerId) {
		this.f = f;
		this.groupLayerId = groupLayerId;
		
		withGroupStructure = tx.query().withRecursive(groupStructure, 
			groupStructure.groupLayerIdentification,
			groupStructure.childLayerId, 
			groupStructure.childLayerIdentification,
			groupStructure.parentLayerId,
			groupStructure.parentLayerIdentification,
			groupStructure.layerOrder).as(
			new SQLSubQuery().unionAll(
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(genericLayer).on(genericLayer.id.eq(layerStructure.parentLayerId))						
					.list(
						genericLayer.identification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder),
				new SQLSubQuery().from(layerStructure)
					.join(child).on(child.id.eq(layerStructure.childLayerId))
					.join(parent).on(parent.id.eq(layerStructure.parentLayerId))
					.join(groupStructure).on(groupStructure.childLayerId.eq(layerStructure.parentLayerId))
					.list(
						groupStructure.groupLayerIdentification, 
						child.id,
						child.identification,
						parent.id,
						parent.identification,
						layerStructure.layerOrder)));
	}
	
	private CompletableFuture<TypedList<Tuple>> structure() {
		return withGroupStructure.clone()
			.from(groupStructure)
			.where(groupStructure.groupLayerIdentification.eq(groupLayerId))
			.orderBy(groupStructure.layerOrder.asc())
			.list(groupStructure.childLayerIdentification, groupStructure.parentLayerIdentification);
	}
	
	private CompletableFuture<Map<Integer, List<String>>> tilingGroupMimeFormats() {
		return withGroupStructure.clone()
			.from(genericLayer)
			.join(groupStructure).on(groupStructure.childLayerId.eq(genericLayer.id))
			.join(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))
			.join(tiledLayerMimeformat).on(tiledLayerMimeformat.tiledLayerId.eq(tiledLayer.id))
			.where(new SQLSubQuery().from(leafLayer)
				.where(leafLayer.genericLayerId.eq(genericLayer.id))
				.notExists())	
			.where(groupStructure.groupLayerIdentification.eq(groupLayerId)
				.or(groupStructure.childLayerIdentification.eq(groupLayerId))) // requested root group
			.list(
				genericLayer.id,
				tiledLayerMimeformat.mimeformat).thenApply(resp -> 
					resp.list().stream()
						.collect(Collectors.groupingBy(t ->
							t.get(genericLayer.id),
							Collectors.mapping(t ->
								t.get(tiledLayerMimeformat.mimeformat),
								Collectors.toList()))));
	}
	
	private CompletableFuture<TypedList<Tuple>> groupInfo() {
		return withGroupStructure.clone()
			.from(genericLayer)
			.join(groupStructure).on(groupStructure.childLayerId.eq(genericLayer.id))
			.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id)) // = optional
			.where(new SQLSubQuery().from(leafLayer)
				.where(leafLayer.genericLayerId.eq(genericLayer.id))
				.notExists())	
			.where(groupStructure.groupLayerIdentification.eq(groupLayerId)
				.or(groupStructure.childLayerIdentification.eq(groupLayerId))) // requested root group
			.list(
				genericLayer.id,
				genericLayer.identification, 
				genericLayer.name, 
				genericLayer.title, 
				genericLayer.abstractCol,
				tiledLayer.genericLayerId,
				tiledLayer.metaWidth,					
				tiledLayer.metaHeight,
				tiledLayer.expireCache,
				tiledLayer.expireClients,
				tiledLayer.gutter);
	}
	
	private CompletableFuture<TypedList<PartialGroupLayer>> groups() {
		return tilingGroupMimeFormats().thenCompose(tilingMimeFormats -> 
			groupInfo().thenApply(resp -> 
				new TypedList<>(PartialGroupLayer.class, resp.list().stream()
					.map(t -> 
						new PartialGroupLayer(
							t.get(genericLayer.identification),
							t.get(genericLayer.name),
							t.get(genericLayer.title),
							t.get(genericLayer.abstractCol),							
							t.get(tiledLayer.genericLayerId) == null ? null
								: new DefaultTiling(
									tilingMimeFormats.get(t.get(genericLayer.id)),
									t.get(tiledLayer.metaWidth),
									t.get(tiledLayer.metaHeight),
									t.get(tiledLayer.expireCache),
									t.get(tiledLayer.expireClients),
									t.get(tiledLayer.gutter))))
					.collect(Collectors.toList()))));
	}
	
	private CompletableFuture<TypedList<DefaultDatasetLayer>> datasets() {
		return withGroupStructure  
			.from(leafLayer)
			.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
			.join(dataset).on(dataset.id.eq(leafLayer.datasetId))
			.join(groupStructure).on(groupStructure.childLayerId.eq(genericLayer.id))
			.where(groupStructure.groupLayerIdentification.eq(groupLayerId))
			.list(
				genericLayer.identification,
				genericLayer.name,
				genericLayer.title,
				genericLayer.abstractCol,
				dataset.name).thenApply(resp ->
					new TypedList<>(DefaultDatasetLayer.class, resp.list().stream()
						.map(t -> new DefaultDatasetLayer(
							t.get(genericLayer.identification),
							t.get(genericLayer.name),
							t.get(genericLayer.title),
							t.get(genericLayer.abstractCol),
							null,
							Collections.emptyList(),
							t.get(dataset.name),
							Collections.emptyList()))
						.collect(Collectors.toList())));
	}

	@Override
	CompletableFuture<Object> result() {
		return groups().thenCompose(groups ->
			groups.list().isEmpty() ? f.successful(new NotFound()) : 
				structure().thenCompose(structure ->															
				datasets().thenApply(datasets -> {							
					// LinkedHashMap is used to preserve layer order
					Map<String, String> structureMap = new LinkedHashMap<>();
					
					Map<String, String> styleMap = new HashMap<>();
					
					for(Tuple structureTuple : structure) {
						structureMap.put(
							structureTuple.get(groupStructure.childLayerIdentification),
							structureTuple.get(groupStructure.parentLayerIdentification));
					}
	
					return DefaultGroupLayer.newInstance(
						groupLayerId,
						datasets.list(),
						groups.list(),
						structureMap,
						styleMap);
				})));
	}

}
