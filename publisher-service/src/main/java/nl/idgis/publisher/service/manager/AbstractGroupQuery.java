package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.database.QTiledLayerMimeformat.tiledLayerMimeformat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.domain.web.tree.DefaultTiling;
import nl.idgis.publisher.domain.web.tree.PartialGroupLayer;

import nl.idgis.publisher.utils.TypedList;

public abstract class AbstractGroupQuery extends AbstractQuery<TypedList<PartialGroupLayer>> {
	
	AbstractGroupQuery(LoggingAdapter log) {
		super(log);
	}

	@Override
	CompletableFuture<TypedList<PartialGroupLayer>> result() {
		return tilingGroupMimeFormats().thenCompose(tilingMimeFormats -> 
			groupInfo().thenApply(resp ->
				new TypedList<>(PartialGroupLayer.class, resp.list().stream()
					.map(t -> {
						String userGroupsString = t.get(genericLayer.usergroups);
						
						userGroupsString = userGroupsString.substring(1, userGroupsString.length() - 1);
						String[] userGroupsArray = userGroupsString.split(",");
						List<String> userGroups = new ArrayList<>();
						for(String userGroup : userGroupsArray) {
							if(!userGroup.trim().isEmpty()) userGroups.add(userGroup);
						}
						
						return new PartialGroupLayer(
							t.get(genericLayer.identification),
							t.get(genericLayer.name),
							t.get(genericLayer.title),
							t.get(genericLayer.abstractCol),
							userGroups,
							t.get(tiledLayer.genericLayerId) == null 
								? Optional.empty()
								: Optional.of(new DefaultTiling(
									tilingMimeFormats.get(t.get(genericLayer.id)),
									t.get(tiledLayer.metaWidth),
									t.get(tiledLayer.metaHeight),
									t.get(tiledLayer.expireCache),
									t.get(tiledLayer.expireClients),
									t.get(tiledLayer.gutter))));
					})
					.collect(Collectors.toList()))));
	}
	
	private CompletableFuture<TypedList<Tuple>> groupInfo() {
		return groups()
			.list(
				genericLayer.id,
				genericLayer.identification, 
				genericLayer.name, 
				genericLayer.title, 
				genericLayer.abstractCol,
				genericLayer.usergroups,
				tiledLayer.genericLayerId,
				tiledLayer.metaWidth,
				tiledLayer.metaHeight,
				tiledLayer.expireCache,
				tiledLayer.expireClients,
				tiledLayer.gutter);
	}
	
	private CompletableFuture<Map<Integer, List<String>>> tilingGroupMimeFormats() {
		return groups()
			.join(tiledLayerMimeformat).on(tiledLayerMimeformat.tiledLayerId.eq(tiledLayer.id))
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

	protected abstract AsyncSQLQuery groups();

}
