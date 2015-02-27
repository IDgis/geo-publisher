package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;

import nl.idgis.publisher.domain.web.tree.DefaultTiling;
import nl.idgis.publisher.domain.web.tree.PartialGroupLayer;

import nl.idgis.publisher.utils.TypedList;

public abstract class AbstractGroupQuery extends AbstractQuery<TypedList<PartialGroupLayer>> {
	
	protected abstract CompletableFuture<Map<Integer, List<String>>> tilingGroupMimeFormats();
	
	protected abstract CompletableFuture<TypedList<Tuple>> groupInfo();

	@Override
	CompletableFuture<TypedList<PartialGroupLayer>> result() {
		return tilingGroupMimeFormats().thenCompose(tilingMimeFormats -> 
			groupInfo().thenApply(resp ->
				new TypedList<>(PartialGroupLayer.class, resp.list().stream()
					.map(t -> new PartialGroupLayer(
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

}
