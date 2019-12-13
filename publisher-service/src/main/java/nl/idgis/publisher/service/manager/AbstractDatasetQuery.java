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
import static nl.idgis.publisher.database.QJobState.jobState;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.path.PathBuilder;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.domain.job.JobState;
import nl.idgis.publisher.domain.web.tree.AbstractDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultRasterDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultVectorDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultStyleRef;
import nl.idgis.publisher.domain.web.tree.DefaultTiling;
import nl.idgis.publisher.domain.web.tree.StyleRef;
import nl.idgis.publisher.domain.web.tree.Tiling;

import nl.idgis.publisher.utils.TypedList;

public abstract class AbstractDatasetQuery extends AbstractQuery<TypedList<AbstractDatasetLayer>> {
	
	protected abstract AsyncSQLQuery filter(AsyncSQLQuery query);
	
	protected final AsyncSQLQuery query;
	
	private PathBuilder<Boolean> confidentialPath = new PathBuilder<> (Boolean.class, "confidential");
	private PathBuilder<Boolean> wmsOnlyPath = new PathBuilder<> (Boolean.class, "wmsOnly");
	
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
			.orderBy(layerStyle.styleOrder.asc())
			.distinct()
			.list(
				layerStyle.styleOrder,
				genericLayer.id,
				style.identification,
				style.name).<Map<Integer, List<StyleRef>>>thenApply(resp ->
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
			.join(importJob).on(importJob.id.eq(
				new SQLSubQuery().from(importJob)
				.join(jobState).on(jobState.jobId.eq(importJob.jobId))
				.where(
					importJob.datasetId.eq(dataset.id)
					.and(jobState.state.eq(JobState.SUCCEEDED.name())))
				.groupBy(importJob.datasetId)
				.unique(importJob.id.max())))
			.join(jobState).on(jobState.jobId.eq(importJob.jobId)
				.and(jobState.state.eq(JobState.SUCCEEDED.name())))
			.join(sourceDatasetVersion).on(sourceDatasetVersion.id.eq(importJob.sourceDatasetVersionId))
			.list(
				genericLayer.id,
				genericLayer.identification, 
				genericLayer.name, 
				genericLayer.title, 
				genericLayer.abstractCol,
				genericLayer.usergroups,
				dataset.identification,
				dataset.metadataIdentification,
				dataset.metadataFileIdentification,
				tiledLayer.genericLayerId,
				tiledLayer.metaWidth,
				tiledLayer.metaHeight,
				tiledLayer.expireCache,
				tiledLayer.expireClients,
				tiledLayer.gutter,
				sourceDatasetVersion.type,
				jobState.createTime,
				new SQLSubQuery ()
					.from (sourceDataset)
					.join (sourceDatasetVersion).on (sourceDatasetVersion.sourceDatasetId.eq (sourceDataset.id))
					.where (dataset.sourceDatasetId.eq (sourceDataset.id))
					.orderBy (sourceDatasetVersion.createTime.desc ())
					.limit (1)
					.list (sourceDatasetVersion.confidential)
					.as (confidentialPath),
				new SQLSubQuery ()
					.from (sourceDataset)
					.join (sourceDatasetVersion).on (sourceDatasetVersion.sourceDatasetId.eq (sourceDataset.id))
					.where (dataset.sourceDatasetId.eq (sourceDataset.id))
					.orderBy (sourceDatasetVersion.createTime.desc ())
					.limit (1)
					.list (sourceDatasetVersion.wmsOnly)
					.as (wmsOnlyPath)
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
			.groupBy(genericLayer.id, importJobColumn.name, importJobColumn.index)
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
	protected CompletableFuture<TypedList<AbstractDatasetLayer>> result() {
		return 
			tilingDatasetMimeFormats().thenCompose(tilingMimeFormats ->
			datasetColumns().thenCompose(columns ->
			datasetKeywords().thenCompose(keywords ->
			datasetStyles().thenCompose(styles ->			
			datasetInfo().thenApply(resp ->
				new TypedList<>(AbstractDatasetLayer.class,
					resp.list().stream()
						.map(t -> {
							String metadataFileIdentification = t.get(dataset.metadataFileIdentification);
							
							String type = t.get(sourceDatasetVersion.type);
							
							String id = t.get(genericLayer.identification);
							String name = t.get(genericLayer.name);	
							String title = t.get(genericLayer.title);
							String abstr = t.get(genericLayer.abstractCol);	
							
							String userGroupsString = t.get(genericLayer.usergroups);
							
							userGroupsString = userGroupsString.substring(1, userGroupsString.length() - 1);
							List<String> userGroups = Arrays.asList(userGroupsString.split(","));
							
							Tiling tiling = getTiling(tilingMimeFormats, t);
							boolean confidential = t.get (confidentialPath);
							boolean wmsOnly = t.get (wmsOnlyPath);
							Timestamp importTime = t.get(jobState.createTime);
							
							log.debug("importTime: {}", importTime);
							
							switch(type) {
								case "RASTER": 
									String fileName = t.get(dataset.identification) + ".tif";
									
									return new DefaultRasterDatasetLayer(
											id,
											name,
											title,
											abstr,
											tiling,
											Optional.ofNullable (metadataFileIdentification),
											getList(keywords, t),
											userGroups,
											fileName,
											getStyleRefs(styles, t),
											confidential,
											wmsOnly,
											importTime);
							
								case "VECTOR":
									String tableName = t.get(dataset.identification); 
									
									return new DefaultVectorDatasetLayer(
										id,
										name,
										title,
										abstr,
										tiling,
										Optional.ofNullable (metadataFileIdentification),
										getList(keywords, t),
										userGroups,
										tableName,
										getList(columns, t),
										getStyleRefs(styles, t),
										confidential,
										wmsOnly,
										importTime);
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
