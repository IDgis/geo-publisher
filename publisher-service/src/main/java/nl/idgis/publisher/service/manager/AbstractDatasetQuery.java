package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QLeafLayerKeyword.leafLayerKeyword;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.database.QTiledLayerMimeformat.tiledLayerMimeformat;
import static nl.idgis.publisher.database.QLastImportJob.lastImportJob;
import static nl.idgis.publisher.database.QImportJob.importJob;
import static nl.idgis.publisher.database.QImportJobColumn.importJobColumn;
import static nl.idgis.publisher.database.QLastSourceDatasetVersion.lastSourceDatasetVersion;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.domain.web.tree.DefaultVectorDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultStyleRef;
import nl.idgis.publisher.domain.web.tree.DefaultTiling;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.Tiling;

import nl.idgis.publisher.utils.TypedList;

public abstract class AbstractDatasetQuery extends AbstractQuery<TypedList<DefaultVectorDatasetLayer>> {
	
	protected abstract AsyncSQLQuery filter(AsyncSQLQuery query);
	
	protected final AsyncSQLQuery query;
	
	AbstractDatasetQuery(LoggingAdapter log, AsyncSQLQuery query) {
		super(log);
		
		this.query = query;
	}	
	
	private CompletableFuture<Map<Integer, List<String>>> tilingDatasetMimeFormats() {
		return filter(query.clone()
			.from(leafLayer)
			.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))				
			.join(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))
			.join(tiledLayerMimeformat).on(tiledLayerMimeformat.tiledLayerId.eq(tiledLayer.id)))
			.distinct()
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
	
	private CompletableFuture<Map<Integer, List<StyleRef>>> datasetStyles() {
		return filter(query.clone()
			.from(leafLayer)
			.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
			.join(layerStyle).on(layerStyle.layerId.eq(leafLayer.id))
			.join(style).on(style.id.eq(layerStyle.styleId)))
			.distinct()
			.list(
				genericLayer.id,
				style.identification,
				style.name).thenApply(resp ->
					resp.list().stream()
						.collect(Collectors.groupingBy(t ->
							t.get(genericLayer.id),
							Collectors.mapping(t ->
								new DefaultStyleRef(
									t.get(style.identification),
									t.get(style.name)),
								Collectors.toList()))));
	}
	
	private CompletableFuture<Map<Integer, List<String>>> datasetKeywords() {
		return filter(query.clone()
			.from(leafLayer)
			.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))				
			.join(leafLayerKeyword).on(leafLayerKeyword.leafLayerId.eq(leafLayer.id)))				
			.distinct()
			.list(
				genericLayer.id,
				leafLayerKeyword.keyword).thenApply(resp ->
					resp.list().stream()
						.collect(Collectors.groupingBy(t ->
							t.get(genericLayer.id),
							Collectors.mapping(t ->
								t.get(leafLayerKeyword.keyword),
								Collectors.toList()))));
	}
	
	private CompletableFuture<TypedList<Tuple>> datasetInfo() {
		return filter(query.clone()
			.from(leafLayer)
			.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
			.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id)) // optional
			.join(dataset).on(dataset.id.eq(leafLayer.datasetId)))
			.join(lastSourceDatasetVersion).on(lastSourceDatasetVersion.datasetId.eq(dataset.id))
			.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(lastSourceDatasetVersion.sourceDatasetVersionId))
			.list(
				genericLayer.id,
				genericLayer.identification, 
				genericLayer.name, 
				genericLayer.title, 
				genericLayer.abstractCol,
				dataset.identification,
				tiledLayer.genericLayerId,
				tiledLayer.metaWidth,					
				tiledLayer.metaHeight,
				tiledLayer.expireCache,
				tiledLayer.expireClients,
				tiledLayer.gutter,
				sourceDatasetVersion.type);
	}
	
	private CompletableFuture<Map<Integer, List<String>>> datasetColumns() {
		// TODO: staging vs publication?
		
		return filter(query.clone()
			.from(leafLayer)
			.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))			
			.join(lastImportJob).on(lastImportJob.datasetId.eq(leafLayer.datasetId))
			.join(importJob).on(importJob.jobId.eq(lastImportJob.jobId))
			.join(importJobColumn).on(importJobColumn.importJobId.eq(importJob.id)))
			.orderBy(importJobColumn.index.asc())
			.list(
				genericLayer.id,
				importJobColumn.name).thenApply(resp ->
					resp.list().stream()
					.collect(Collectors.groupingBy(t ->
						t.get(genericLayer.id),
						Collectors.mapping(t ->
							t.get(importJobColumn.name),
							Collectors.toList()))));
	}

	@Override
	protected CompletableFuture<TypedList<DefaultVectorDatasetLayer>> result() {
		return 
			tilingDatasetMimeFormats().thenCompose(tilingMimeFormats ->
			datasetColumns().thenCompose(columns ->
			datasetKeywords().thenCompose(keywords ->
			datasetStyles().thenCompose(styles ->			
			datasetInfo().thenApply(resp ->
				new TypedList<>(DefaultVectorDatasetLayer.class, 
					resp.list().stream()
						.map(t -> {
							String type = t.get(sourceDatasetVersion.type);
							
							switch(type) {
								// TODO: add raster data support
							
								case "VECTOR":
									return new DefaultVectorDatasetLayer(
										t.get(genericLayer.identification),
										t.get(genericLayer.name),
										t.get(genericLayer.title),
										t.get(genericLayer.abstractCol),								
										getTiling(tilingMimeFormats, t),
										getList(keywords, t),
										t.get(dataset.identification),
										getList(columns, t),
										getStyleRefs(styles, t));
									
								default:
									throw new IllegalStateException("unknown dataset type: " + type);
							}
						})
						.collect(Collectors.toList())))))));
	}

	private List<StyleRef> getStyleRefs(Map<Integer, List<StyleRef>> styles, Tuple t) {
		return styles.containsKey(t.get(genericLayer.id))
			? styles.get(t.get(genericLayer.id))
			: Collections.emptyList();
	}

	private <T> List<T> getList(Map<Integer, List<T>> keywords, Tuple t) {
		return keywords.containsKey(t.get(genericLayer.id)) 
			? keywords.get(t.get(genericLayer.id))
			: Collections.emptyList();
	}

	private Tiling getTiling(Map<Integer, List<String>> tilingMimeFormats, Tuple t) {
		return t.get(tiledLayer.genericLayerId) == null 
			? null
			: new DefaultTiling(
				tilingMimeFormats.get(t.get(genericLayer.id)),
				t.get(tiledLayer.metaWidth),
				t.get(tiledLayer.metaHeight),
				t.get(tiledLayer.expireCache),
				t.get(tiledLayer.expireClients),
				t.get(tiledLayer.gutter));
	}
}
