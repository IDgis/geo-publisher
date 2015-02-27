package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QConstants.constants;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QLeafLayerKeyword.leafLayerKeyword;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QServiceKeyword.serviceKeyword;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.database.QTiledLayerMimeformat.tiledLayerMimeformat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;

import nl.idgis.publisher.database.AsyncHelper;

import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DefaultDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultService;
import nl.idgis.publisher.domain.web.tree.PartialGroupLayer;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class GetServiceQuery extends AbstractServiceQuery<Object> {
	
	private class GroupQuery extends AbstractGroupQuery {

		@Override
		protected CompletableFuture<TypedList<Tuple>> groupInfo() {
			return withServiceStructure.clone()
				.from(genericLayer)
				.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
				.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id)) // = optional
				.where(new SQLSubQuery().from(leafLayer)
					.where(leafLayer.genericLayerId.eq(genericLayer.id))
					.notExists())	
				.where(serviceStructure.serviceIdentification.eq(serviceId))
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
		
		@Override
		protected CompletableFuture<Map<Integer, List<String>>> tilingGroupMimeFormats() {
			return withServiceStructure.clone()
				.from(genericLayer)
				.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
				.join(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))
				.join(tiledLayerMimeformat).on(tiledLayerMimeformat.tiledLayerId.eq(tiledLayer.id))
				.where(new SQLSubQuery().from(leafLayer)
					.where(leafLayer.genericLayerId.eq(genericLayer.id))
					.notExists())	
				.where(serviceStructure.serviceIdentification.eq(serviceId))
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
		
	}
	
	private class DatasetQuery extends AbstractDatasetQuery {
		@Override
		protected CompletableFuture<Map<Integer, List<String>>> tilingDatasetMimeFormats() {
			return withServiceStructure.clone()
				.from(leafLayer)
				.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))				
				.join(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))
				.join(tiledLayerMimeformat).on(tiledLayerMimeformat.tiledLayerId.eq(tiledLayer.id))
				.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
				.where(serviceStructure.serviceIdentification.eq(serviceId))
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
		
		@Override
		protected CompletableFuture<Map<Integer, List<String>>> datasetStyles() {
			return withServiceStructure.clone()
				.from(leafLayer)
				.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
				.join(layerStyle).on(layerStyle.layerId.eq(leafLayer.id))
				.join(style).on(style.id.eq(layerStyle.styleId))
				.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
				.where(serviceStructure.serviceIdentification.eq(serviceId))
				.list(
					genericLayer.id,
					style.identification).thenApply(resp ->
						resp.list().stream()
							.collect(Collectors.groupingBy(t ->
								t.get(genericLayer.id),
								Collectors.mapping(t ->
									t.get(style.identification),
									Collectors.toList()))));
		}
		
		@Override
		protected CompletableFuture<Map<Integer, List<String>>> datasetKeywords() {
			return withServiceStructure.clone()
				.from(leafLayer)
				.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))				
				.join(leafLayerKeyword).on(leafLayerKeyword.leafLayerId.eq(leafLayer.id))
				.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
				.where(serviceStructure.serviceIdentification.eq(serviceId))
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
		
		@Override
		protected CompletableFuture<TypedList<Tuple>> datasetInfo() {
			return withServiceStructure  
				.from(leafLayer)
				.join(genericLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
				.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id)) // optional
				.join(dataset).on(dataset.id.eq(leafLayer.datasetId))
				.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
				.where(serviceStructure.serviceIdentification.eq(serviceId))
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
					tiledLayer.gutter);
		}
	}
	
	private final String serviceId;

	GetServiceQuery(FutureUtils f, AsyncHelper tx, String serviceId) {
		super(f, tx);
		
		this.serviceId = serviceId;
	}
	
	private CompletableFuture<TypedList<Tuple>> structure() {
		return withServiceStructure.clone()
			.from(serviceStructure)
			.where(serviceStructure.serviceIdentification.eq(serviceId))
			.orderBy(serviceStructure.layerOrder.asc())
			.list(
				serviceStructure.styleIdentification,
				serviceStructure.childLayerIdentification, 
				serviceStructure.parentLayerIdentification);
	}
	
	private CompletableFuture<TypedList<PartialGroupLayer>> groups() {
		return new GroupQuery().result();
	}
	
	private CompletableFuture<TypedList<String>> keywords() {
		return tx.query().from(service)
			.join(serviceKeyword).on(serviceKeyword.serviceId.eq(service.id))
			.where(service.identification.eq(serviceId))
			.list(serviceKeyword.keyword);
	}
	
	private CompletableFuture<Optional<Tuple>> info() {
		return tx.query().from(service)
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.leftJoin(constants).on(constants.id.eq(service.constantsId))
			.where(service.identification.eq(serviceId))
			.singleResult(
				service.name,
				service.title,
				service.abstractCol,
				genericLayer.identification, 
				genericLayer.name, 
				genericLayer.title, 
				genericLayer.abstractCol,
				constants.contact,
				constants.organization,
				constants.position,
				constants.addressType,
				constants.address,
				constants.city,
				constants.state,
				constants.zipcode,
				constants.country,
				constants.telephone,
				constants.fax,
				constants.email);
	}
	
	private CompletableFuture<TypedList<DefaultDatasetLayer>> datasets() {
		return new DatasetQuery().result();
	}
	
	@Override
	CompletableFuture<Object> result() {
		return info().thenCompose(info ->
			info.isPresent() ?
				structure().thenCompose(structure ->						
				groups().thenCompose(groups ->
				keywords().thenCompose(keywords ->
				datasets().thenApply(datasets -> {							
					// LinkedHashMap is used to preserve layer order
					Map<String, String> structureMap = new LinkedHashMap<>();
					
					Map<String, String> styleMap = new HashMap<>();
					
					for(Tuple structureTuple : structure) {
						String styleId = structureTuple.get(serviceStructure.styleIdentification);
						String childId = structureTuple.get(serviceStructure.childLayerIdentification);
						String parentId = structureTuple.get(serviceStructure.parentLayerIdentification); 
						
						structureMap.put(childId, parentId);
						if(styleId != null) {
							styleMap.put(childId, styleId);
						}
					}
					
					Tuple serviceInfoTuple = info.get();
	
					return new DefaultService(
						serviceId, 
						serviceInfoTuple.get(service.name),
						serviceInfoTuple.get(service.title),
						serviceInfoTuple.get(service.abstractCol),
						keywords.list(),
						serviceInfoTuple.get(constants.contact),
						serviceInfoTuple.get(constants.organization),
						serviceInfoTuple.get(constants.position),
						serviceInfoTuple.get(constants.addressType),
						serviceInfoTuple.get(constants.address),
						serviceInfoTuple.get(constants.city),
						serviceInfoTuple.get(constants.state),
						serviceInfoTuple.get(constants.zipcode),
						serviceInfoTuple.get(constants.country),
						serviceInfoTuple.get(constants.telephone),
						serviceInfoTuple.get(constants.fax),
						serviceInfoTuple.get(constants.email),
						new PartialGroupLayer(
							serviceInfoTuple.get(genericLayer.identification),
							serviceInfoTuple.get(genericLayer.name),
							serviceInfoTuple.get(genericLayer.title),
							serviceInfoTuple.get(genericLayer.abstractCol),
							null), // a root group doesn't have (or need) tiling
						datasets.list(),
						groups.list(),
						structureMap,
						styleMap);
				}))))
			: f.successful(new NotFound()));
	}
}
