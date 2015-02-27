package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;

import nl.idgis.publisher.domain.web.tree.DefaultDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultTiling;

import nl.idgis.publisher.utils.TypedList;

public abstract class AbstractDatasetQuery extends AbstractQuery<TypedList<DefaultDatasetLayer>> {
	
	protected abstract CompletableFuture<Map<Integer, List<String>>> tilingDatasetMimeFormats();
	
	protected abstract CompletableFuture<Map<Integer, List<String>>> datasetKeywords();
	
	protected abstract CompletableFuture<Map<Integer, List<String>>> datasetStyles();
	
	protected abstract CompletableFuture<TypedList<Tuple>> datasetInfo();

	@Override
	protected CompletableFuture<TypedList<DefaultDatasetLayer>> result() {
		return 
			tilingDatasetMimeFormats().thenCompose(tilingMimeFormats ->
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
							styles.containsKey(t.get(genericLayer.id))
								? styles.get(t.get(genericLayer.id))
								: Collections.emptyList()))
						.collect(Collectors.toList()))))));
	}
}
