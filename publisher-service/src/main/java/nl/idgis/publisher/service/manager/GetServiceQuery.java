package nl.idgis.publisher.service.manager;

import static nl.idgis.publisher.database.QConstants.constants;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QServiceKeyword.serviceKeyword;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;

import akka.event.LoggingAdapter;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;

import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.tree.DefaultDatasetLayer;
import nl.idgis.publisher.domain.web.tree.DefaultService;
import nl.idgis.publisher.domain.web.tree.PartialGroupLayer;

import nl.idgis.publisher.utils.FutureUtils;
import nl.idgis.publisher.utils.TypedList;

public class GetServiceQuery extends AbstractServiceQuery<Object> {
	
	private class GroupQuery extends AbstractGroupQuery {

		GroupQuery(LoggingAdapter log) {
			super(log);
		}

		@Override
		protected AsyncSQLQuery groups() {
			return withServiceStructure.clone()
				.from(genericLayer)
				.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
				.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))				
				.where(new SQLSubQuery().from(leafLayer)
					.where(leafLayer.genericLayerId.eq(genericLayer.id))
					.notExists())	
				.where(serviceStructure.serviceIdentification.eq(serviceId));
		}		
	}
	
	private class DatasetQuery extends AbstractDatasetQuery {
		
		DatasetQuery(LoggingAdapter log) {
			super(log, withServiceStructure);
		}
		
		@Override
		protected AsyncSQLQuery filter(AsyncSQLQuery query) {
			return 
				query
					.join(serviceStructure).on(serviceStructure.childLayerId.eq(genericLayer.id))
					.where(serviceStructure.serviceIdentification.eq(serviceId));
		}
	}
	
	private final String serviceId;

	GetServiceQuery(LoggingAdapter log, FutureUtils f, AsyncHelper tx, String serviceId) {		
		super(log, f, tx);
		
		this.serviceId = serviceId;
	}
	
	private CompletableFuture<TypedList<Tuple>> structure() {
		return withServiceStructure.clone()
			.from(serviceStructure)
			.where(serviceStructure.serviceIdentification.eq(serviceId))
			// order by parentLayerId is required in order to be able eliminate duplicates
			.orderBy(serviceStructure.parentLayerId.asc(), serviceStructure.layerOrder.asc())
			.list(
				serviceStructure.styleIdentification,
				serviceStructure.styleName,
				serviceStructure.childLayerIdentification, 
				serviceStructure.parentLayerIdentification,
				serviceStructure.layerOrder,
				serviceStructure.cycle);
	}
	
	private CompletableFuture<TypedList<PartialGroupLayer>> groups() {
		return new GroupQuery(log).result();
	}
	
	private CompletableFuture<TypedList<String>> keywords() {
		return tx.query().from(service)
			.join(serviceKeyword).on(serviceKeyword.serviceId.eq(service.id))
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.list(serviceKeyword.keyword);
	}
	
	private CompletableFuture<Optional<Tuple>> info() {
		return tx.query().from(service)
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.leftJoin(constants).on(constants.id.eq(service.constantsId))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(
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
		return new DatasetQuery(log).result();
	}
	
	@Override
	CompletableFuture<Object> result() {
		StructureProcessor structureProcessor = new StructureProcessor(
				serviceStructure.styleIdentification,
				serviceStructure.styleName,
				serviceStructure.childLayerIdentification,
				serviceStructure.parentLayerIdentification,
				serviceStructure.layerOrder,
				serviceStructure.cycle);
		
		return info().thenCompose(info ->
			info.isPresent() ?
				structure().thenCompose(structure ->						
				groups().thenCompose(groups ->
				keywords().thenCompose(keywords ->
				datasets().thenApply(datasets -> {
					StructureProcessor.Result transformedStructure 
						= structureProcessor.transform(structure.list());
					
					Tuple serviceInfoTuple = info.get();
	
					return new DefaultService(
						serviceId, 
						serviceInfoTuple.get(genericLayer.name),
						serviceInfoTuple.get(genericLayer.title),
						serviceInfoTuple.get(genericLayer.abstractCol),
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
						transformedStructure.getStructureItems(),
						transformedStructure.getStyles());
				}))))
			: f.successful(new NotFound()));
	}
}
