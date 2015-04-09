package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.path.PathBuilder;

import akka.event.LoggingAdapter;
import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.domain.web.tree.DefaultDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultStyleRef;
import nl.idgis.publisher.domain.web.tree.DefaultTiling;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.utils.TypedList;

public abstract class AbstractDatasetQuery extends AbstractQuery<TypedList<DefaultDatasetLayer>> {
	
	protected abstract AsyncSQLQuery filter(AsyncSQLQuery query);
	
	protected final AsyncSQLQuery query;
	
	private PathBuilder<Boolean> confidentialPath = new PathBuilder<> (Boolean.class, "confidential");
	
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
				new SQLSubQuery ()
					.from (sourceDataset)
					.join (sourceDatasetVersion).on (sourceDatasetVersion.sourceDatasetId.eq (sourceDataset.id))
					.where (dataset.sourceDatasetId.eq (sourceDataset.id))
					.orderBy (sourceDatasetVersion.createTime.desc ())
					.limit (1)
					.list (sourceDatasetVersion.confidential)
					.as (confidentialPath)
			);
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
	protected CompletableFuture<TypedList<DefaultDatasetLayer>> result() {
		return 
			tilingDatasetMimeFormats().thenCompose(tilingMimeFormats ->
			datasetColumns().thenCompose(columns ->
			datasetKeywords().thenCompose(keywords ->
			datasetStyles().thenCompose(styles ->			
			datasetInfo().thenApply(resp ->
				new TypedList<>(DefaultDatasetLayer.class, 
					resp.list().stream()
						.map(t -> new DefaultDatasetLayer(
							t.get(genericLayer.identification),
							t.get(genericLayer.name),
							t.get(genericLayer.title),
							t.get(genericLayer.abstractCol),								
							t.get(tiledLayer.genericLayerId) == null 
								? null
								: new DefaultTiling(
									tilingMimeFormats.get(t.get(genericLayer.id)),
									t.get(tiledLayer.metaWidth),
									t.get(tiledLayer.metaHeight),
									t.get(tiledLayer.expireCache),
									t.get(tiledLayer.expireClients),
									t.get(tiledLayer.gutter)),
							keywords.containsKey(t.get(genericLayer.id)) 
								? keywords.get(t.get(genericLayer.id))									
								: Collections.emptyList(),
							t.get(dataset.identification),
							columns.containsKey(t.get(genericLayer.id))
								? columns.get(t.get(genericLayer.id))
								: Collections.emptyList(),
							styles.containsKey(t.get(genericLayer.id))
								? styles.get(t.get(genericLayer.id))
								: Collections.emptyList(),
							t.get (confidentialPath)
						))
						.collect(Collectors.toList())))))));
	}
}
