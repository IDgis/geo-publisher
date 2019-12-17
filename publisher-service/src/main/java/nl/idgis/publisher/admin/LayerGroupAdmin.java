package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.database.QTiledLayerMimeformat.tiledLayerMimeformat;
import static nl.idgis.publisher.service.manager.QGroupStructure.groupStructure;
import static nl.idgis.publisher.service.manager.QGroupStructure.withGroupStructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Ops;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.PathBuilder;

import akka.actor.ActorRef;
import akka.actor.Props;
import nl.idgis.publisher.data.GenericLayer;
import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QGenericLayer;
import nl.idgis.publisher.database.QLeafLayer;
import nl.idgis.publisher.domain.query.GetGroupLayerRef;
import nl.idgis.publisher.domain.query.GetGroupParentGroups;
import nl.idgis.publisher.domain.query.GetGroupParentServices;
import nl.idgis.publisher.domain.query.GetGroupStructure;
import nl.idgis.publisher.domain.query.GetLayerRef;
import nl.idgis.publisher.domain.query.GetLayerServices;
import nl.idgis.publisher.domain.query.ListLayerGroups;
import nl.idgis.publisher.domain.query.PutGroupStructure;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.Service;
import nl.idgis.publisher.domain.web.TiledLayer;
import nl.idgis.publisher.domain.web.tree.DefaultGroupLayerRef;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.domain.web.tree.LayerRef;
import nl.idgis.publisher.protocol.messages.Failure;
import nl.idgis.publisher.service.manager.CycleException;
import nl.idgis.publisher.service.manager.messages.GetDatasetLayerRef;
import nl.idgis.publisher.service.manager.messages.GetGroupLayer;
import nl.idgis.publisher.service.manager.messages.GetServicesWithLayer;
import nl.idgis.publisher.utils.StreamUtils;
import nl.idgis.publisher.utils.StreamUtils.ZippedEntry;
import nl.idgis.publisher.utils.TypedIterable;
import nl.idgis.publisher.utils.TypedList;

public class LayerGroupAdmin extends LayerGroupCommonAdmin {
	
	protected final static QGenericLayer child = new QGenericLayer("child"), parent = new QGenericLayer("parent");
	
	private final ActorRef serviceManager;

	private final PathBuilder<Boolean> confidentialPath = new PathBuilder<> (Boolean.class, "confidential");
	private final PathBuilder<Boolean> wmsOnlyPath = new PathBuilder<> (Boolean.class, "wmsOnly");
	
	public LayerGroupAdmin(ActorRef database, ActorRef serviceManager) {
		super(database); 
		
		this.serviceManager = serviceManager;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager) {
		return Props.create(LayerGroupAdmin.class, database, serviceManager);
	}

	@Override
	protected void preStartAdmin() {
		doQuery(GetLayerRef.class, this::handleGetLayerRef);
		doQuery(GetGroupLayerRef.class, this::handleGetLayerGroupRef);
		
		doList(LayerGroup.class, this::handleListLayergroups);
		doGet(LayerGroup.class, this::handleGetLayergroup);
		doPut(LayerGroup.class, this::handlePutLayergroup);
		doDelete(LayerGroup.class, this::handleDeleteLayergroup);
		
		doQueryOptional(GetLayerServices.class, this::handleGetLayerServices);
		
		doQueryOptional(GetGroupStructure.class, this::handleGetGroupStructure);
		doQuery(PutGroupStructure.class, this::handlePutGroupStructure);
		
		doQuery (ListLayerGroups.class, this::handleListLayerGroupsWithQuery);

		doQuery (GetGroupParentServices.class, this::handleGetGroupParentServices);
		doQuery (GetGroupParentGroups.class, this::handleGetGroupParentGroups);
		
	}
	
	private CompletableFuture<LayerRef<?>> handleGetLayerGroupRef(GetGroupLayerRef getLayerGroupRef) {
		String groupId = getLayerGroupRef.getGroupId();
		
		log.debug("handleGetLayerGroupRef: {}", groupId);
		
		return f.ask(serviceManager, new GetGroupLayer(groupId), GroupLayer.class)
			.thenApply(groupLayer -> new DefaultGroupLayerRef(groupLayer));
	}
	
	private CompletableFuture<LayerRef<?>> handleGetLayerRef(GetLayerRef getLayerRef) {
		String layerId = getLayerRef.getLayerId();
		
		log.debug("handleGetLayerRef: {}", layerId);
		
		return f.ask(serviceManager, new GetDatasetLayerRef(layerId), LayerRef.class)
			.thenApply(layerRef -> layerRef);
	}

	private CompletableFuture<Optional<List<String>>> handleGetLayerServices (GetLayerServices getLayerServices) {
		log.debug ("handleGetLayerServices of layer: " + getLayerServices.layerId());
		return f.ask(this.serviceManager, new GetServicesWithLayer(getLayerServices.layerId())).thenApply(resp -> {
			if(resp instanceof NotFound) {
				return Optional.empty();
			} else if (resp instanceof Failure){
				return Optional.empty();
			} else {
				TypedIterable<String> services = (TypedIterable<String>)resp;
				List<String> serviceList = new ArrayList<String>(services.asCollection());
				return Optional.of(serviceList);
			}
		});
	}

	private CompletableFuture<Optional<GroupLayer>> handleGetGroupStructure (GetGroupStructure getGroupStructure) {
		log.debug ("handleListLayergroups");
		return f.ask(this.serviceManager, new GetGroupLayer(getGroupStructure.groupId())).thenApply(resp -> {
			if(resp instanceof NotFound) {
				return Optional.empty();
			} else if (resp instanceof Failure){
				return Optional.empty();
			} else {
				return Optional.of((GroupLayer)resp);
			}
		});
	}

	private CompletableFuture<Page<LayerGroup>> handleListLayergroups () {
		return handleListLayerGroupsWithQuery (new ListLayerGroups (null, null));
	}

	private BooleanExpression isConfidential () {
		final QLeafLayer leafLayer = new QLeafLayer ("leaf_layer2");
		
		return new SQLSubQuery ()
			.from (groupStructure)
			.join (leafLayer).on (groupStructure.childLayerId.eq (leafLayer.genericLayerId))
			.join (dataset).on (leafLayer.datasetId.eq (dataset.id))
			.join (sourceDataset).on (dataset.sourceDatasetId.eq (sourceDataset.id))
			.where (groupStructure.groupLayerIdentification.eq (genericLayer.identification))
			.where (Expressions.booleanOperation (Ops.EQ, Expressions.constant (true), new SQLSubQuery ().from (sourceDatasetVersion).where (sourceDatasetVersion.sourceDatasetId.eq (sourceDataset.id)).orderBy (sourceDatasetVersion.createTime.desc ()).limit (1).list (sourceDatasetVersion.confidential)))
			.exists ();
	}
	
	private BooleanExpression isWmsOnly () {
		final QLeafLayer leafLayer = new QLeafLayer ("leaf_layer2");
		
		return new SQLSubQuery ()
			.from (groupStructure)
			.join (leafLayer).on (groupStructure.childLayerId.eq (leafLayer.genericLayerId))
			.join (dataset).on (leafLayer.datasetId.eq (dataset.id))
			.join (sourceDataset).on (dataset.sourceDatasetId.eq (sourceDataset.id))
			.where (groupStructure.groupLayerIdentification.eq (genericLayer.identification))
			.where (Expressions.booleanOperation (Ops.EQ, Expressions.constant (true), new SQLSubQuery ().from (sourceDatasetVersion).where (sourceDatasetVersion.sourceDatasetId.eq (sourceDataset.id)).orderBy (sourceDatasetVersion.createTime.desc ()).limit (1).list (sourceDatasetVersion.wmsOnly)))
			.exists ();
	}
	
	private CompletableFuture<Page<LayerGroup>> handleListLayerGroupsWithQuery (final ListLayerGroups listLayerGroups) {
		final AsyncSQLQuery baseQuery = db
			.query()
			.from(genericLayer)
			.leftJoin(leafLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
			.leftJoin(service).on(genericLayer.id.eq(service.genericLayerId))
			.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))
			.where(leafLayer.genericLayerId.isNull().and(service.genericLayerId.isNull()))
			.orderBy (genericLayer.name.asc ());

		// Add a filter for the query string:
		if (listLayerGroups.getQuery () != null) {
			baseQuery.where (
					genericLayer.name.containsIgnoreCase (listLayerGroups.getQuery ())
					.or (genericLayer.title.containsIgnoreCase (listLayerGroups.getQuery()))
				);
		}
		
		final AsyncSQLQuery listQuery = baseQuery.clone ();
		
		singlePage (listQuery, listLayerGroups.getPage ());
		
		withGroupStructure (listQuery, parent, child);
		
		return baseQuery
				.count ()
				.thenCompose ((count) -> {
					final Page.Builder<LayerGroup> builder = new Page.Builder<> ();
					
					addPageInfo (builder, listLayerGroups.getPage (), count);
					
					return listQuery
						.list (
							genericLayer.identification,
							genericLayer.name,
							genericLayer.title,
							genericLayer.abstractCol,
							tiledLayer.genericLayerId,
							isConfidential ().as (confidentialPath),
							isWmsOnly ().as (wmsOnlyPath)
						)
						.thenApply ((groups) -> {
							for (Tuple group : groups.list()) {
								boolean hasTiledLayer = group.get(tiledLayer.genericLayerId) != null;
								log.debug("Group: " + group.get(genericLayer.name) + ", tiling = " + hasTiledLayer);
								builder.add(new LayerGroup(
										group.get(genericLayer.identification),
										group.get(genericLayer.name),
										group.get(genericLayer.title),
										group.get(genericLayer.abstractCol),
										null,
										(hasTiledLayer
											? new TiledLayer(
												group.get(genericLayer.identification),
												group.get(genericLayer.name),
												group.get(null),
												group.get(null),
												group.get(null),
												group.get(null),
												group.get(null),
												null)
											: null),
										group.get (confidentialPath),
										group.get (wmsOnlyPath)
									));
							}
							return builder.build ();
						});
				});
	}
	
	private CompletableFuture<Optional<LayerGroup>> handleGetLayergroup (String layergroupId) {
		log.debug("handleGetLayergroup: " + layergroupId);
		
		List<Expression<?>> groupColumns = new ArrayList<>();
		groupColumns.addAll(Arrays.asList(genericLayer.all()));
		groupColumns.addAll(Arrays.asList(tiledLayer.all()));
		groupColumns.add (isConfidential ().as (confidentialPath));
		groupColumns.add (isWmsOnly ().as (wmsOnlyPath));
		
		return db.transactional(tx -> 
			withGroupStructure (tx.query(), parent, child)
			.from(genericLayer)
			.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))
			.where(genericLayer.identification.eq(layergroupId))
			.singleResult(groupColumns.toArray (new Expression<?>[groupColumns.size ()])).<Optional<LayerGroup>>thenCompose(optionalGroup -> {
				if(optionalGroup.isPresent()) {
					Tuple group = optionalGroup.get();
					log.debug("generic layer id: " + group.get(genericLayer.id));
						log.debug("tiled layer   id: " + group.get(tiledLayer.id));
						boolean hasTiledLayer = group.get(tiledLayer.genericLayerId) != null;
						log.debug("tiled layer glId: " + group.get(tiledLayer.genericLayerId) + " = " + hasTiledLayer);
						
						CompletableFuture<TypedList<String>> mimeformatsQuery;
						if(hasTiledLayer) {
							mimeformatsQuery = tx.query()
							.from(tiledLayerMimeformat)
							.where(tiledLayerMimeformat.tiledLayerId.eq(group.get(tiledLayer.id)))
							.list(tiledLayerMimeformat.mimeformat);
						} else {
							mimeformatsQuery = f.successful(new TypedList<>(String.class, Collections.emptyList()));
						}
						
						return mimeformatsQuery.<Optional<LayerGroup>>thenApply(mimeFormats ->	
							Optional.of(new LayerGroup(
								group.get(genericLayer.identification),
								group.get(genericLayer.name),
								group.get(genericLayer.title),
								group.get(genericLayer.abstractCol),
								GenericLayer.transformUserGroupsToList(group.get(genericLayer.usergroups)),
								(hasTiledLayer
									? new TiledLayer(
										group.get(genericLayer.identification),
										group.get(genericLayer.name),
										group.get(tiledLayer.metaWidth),
										group.get(tiledLayer.metaHeight),
										group.get(tiledLayer.expireCache),
										group.get(tiledLayer.expireClients),
										group.get(tiledLayer.gutter),
										mimeFormats.list())
									: null),
								group.get (confidentialPath),
								group.get (wmsOnlyPath)
							)));
				} else {
					return f.successful(Optional.empty());
				}
			}));
	}
	
	private CompletableFuture<Response<?>> handlePutLayergroup(LayerGroup lg) {
		String layergroupId = lg.id();
		String layergroupName = lg.name();
		List<String> userGroups = lg.userGroups();
		log.debug ("handle update/create layergroup: " + layergroupId);
		
		Collections.sort(userGroups);
		
		return db.transactional(tx ->
			// Check if there is another layergroup with the same id
			tx.query()
				.from(genericLayer)
				.where(genericLayer.identification.eq(layergroupId))
				.singleResult(genericLayer.id)
				.thenCompose(glId -> {
					if (!glId.isPresent()){
						// INSERT
						log.debug("Inserting new layergroup with name: " + layergroupName);
						String newGroupId = UUID.randomUUID().toString();
						return tx.insert(genericLayer)
							.set(genericLayer.identification, newGroupId)
							.set(genericLayer.name, layergroupName)
							.set(genericLayer.title, lg.title())
							.set(genericLayer.abstractCol, lg.abstractText())
							.set(genericLayer.usergroups, GenericLayer.transformUserGroupsToText(userGroups))
							.execute()
							.thenCompose(
								n -> {
									log.debug("Inserted generic_layer: #" + n);
									return tx
										.query()
										.from(genericLayer)
										.where(genericLayer.identification.eq(newGroupId))
										.singleResult(genericLayer.id)
										.thenCompose(
											glId2 -> {
												if (lg.tiledLayer().isPresent()){
													log.debug("Insert tiledlayer ");
													return insertTiledLayer(tx, lg.tiledLayer().get(), glId2.get(), log)
														.thenApply(whatever ->
															new Response<String>(CrudOperation.CREATE,
																CrudResponse.OK, newGroupId));
												} else {
													return f.successful( 
														new Response<String>(CrudOperation.CREATE,
																CrudResponse.OK, newGroupId));
												}
											});
								});
					} else {
						// UPDATE
						log.debug("Updating layergroup with name: " + layergroupName + ", id:" + layergroupId);
						return tx.update(genericLayer)
							.set(genericLayer.title, lg.title())
							.set(genericLayer.abstractCol, lg.abstractText())
							.set(genericLayer.usergroups, GenericLayer.transformUserGroupsToText(userGroups))
							.where(genericLayer.identification.eq(layergroupId))
							.execute()
							.thenCompose(
								gl -> {
									log.debug("updated generic_layer: #" + gl);
										return tx
											.delete(tiledLayer)
											.where(tiledLayer.genericLayerId.eq(glId.get()))
											.execute()
											.thenCompose(
												tlIdOld -> {
												log.debug("Deleted tiledlayer glId: " + glId.get());
												if (lg.tiledLayer().isPresent()){
													return insertTiledLayer(tx, lg.tiledLayer().get(), glId.get(), log)
													    .thenApply(whatever ->
													        new Response<String>(CrudOperation.UPDATE,
											                CrudResponse.OK, layergroupId));
												} else {
													return f.successful( 
														new Response<String>(CrudOperation.UPDATE,
											            CrudResponse.OK, layergroupId));
												}
										});
								});
					}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteLayergroup(String layergroupId) {
		log.debug("handleDeleteLayergroup: " + layergroupId);
		//first check if generic layer is available and is not a service rootgroup
		return db.transactional(tx -> 
			tx.query()
			.from(genericLayer)
			.leftJoin(service).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(layergroupId)
					.and(service.id.isNull())
					)
			.limit(1)
			.singleResult(genericLayer.id)
			.thenCompose(
				glId -> {
					if (glId.isPresent()){
						// remove from layerStructure if present in parent or child
						log.debug("delete layerstructures " + glId.get());
						return 
							tx.delete(layerStructure)
							.where(layerStructure.parentLayerId.eq(glId.get()).or(
								   layerStructure.childLayerId.eq(glId.get())))
							.execute()
							.thenCompose(
								nr -> {
									log.debug("LayerStructures deleted: #" + nr);
									log.debug("delete genericLayer: " + glId.get());
									return 
										tx.delete(genericLayer)
										.where(genericLayer.id.eq(glId.get()))
										.execute()
										.thenApply(
											l -> new Response<String>(CrudOperation.DELETE,
												CrudResponse.OK, layergroupId));
							});
						} else {
							// generic layer id not in table
							log.debug("delete genericLayer failed: " + layergroupId);
							return f.successful(new Response<String>(CrudOperation.DELETE,
									CrudResponse.NOK, layergroupId));
						}
					})
				);
	}
	
	private static class EnrichedCycleException extends RuntimeException {
		
		private final Response<String> response;
		
		public EnrichedCycleException(CycleException cause, Response<String> response) {
			super(cause);
			
			this.response = response;
		}
		
		public Response<String> getResponse() {
			return response;
		}
	}
	
	private CompletableFuture<Response<String>> handlePutGroupStructure (final PutGroupStructure putGroupStructure) {
		String groupId = putGroupStructure.groupId();

		List<String> layerIdList =  putGroupStructure.layerIdList();
		List<String> layerStyleIdList =  putGroupStructure.layerStyleIdList();
		
		if(layerIdList.size() != layerStyleIdList.size()) {
			return f.failed(new IllegalArgumentException("layerId, layerStyleId size mismatch"));
		}
				
		log.debug("handlePutGroupStructure groupId: " + groupId + ", layer id's: " +layerIdList);
		
		return db.<Response<String>>transactional(tx -> tx
			.query()
			.from(genericLayer)
			.where(genericLayer.identification.eq(groupId))
			.singleResult(genericLayer.id)
			.thenCompose(
				glId -> {
				log.debug("genericlayer id: " + glId.get());
					// A. delete the existing structure of this layer
					return tx.delete(layerStructure)
						.where(layerStructure.parentLayerId.eq(glId.get()))
						.execute()
						.thenCompose(
							llNr -> {
								// B. insert items of layerStructure
								return f.sequence(												
									StreamUtils.index(
											StreamUtils.zip(
												layerIdList.stream(), 
												layerStyleIdList.stream()))
										.map(indexed -> {
											ZippedEntry<String, String> value = indexed.getValue();
											
											String layerId = value.getFirst();
											String layerStyleId = value.getSecond();
											
											if(layerStyleId.isEmpty()) {											
												return tx.insert(layerStructure)
													.columns(
														layerStructure.parentLayerId, 
														layerStructure.childLayerId,
														layerStructure.layerOrder)
													.select(new SQLSubQuery().from(genericLayer)
														.where(genericLayer.identification.eq(layerId))
														.list(
															glId.get(),
															genericLayer.id,
															indexed.getIndex()))
													.execute();
											} else {
												return tx.insert(layerStructure)
													.columns(
														layerStructure.parentLayerId, 
														layerStructure.childLayerId,
														layerStructure.layerOrder,
														layerStructure.styleId)
													.select(new SQLSubQuery().from(genericLayer)
														.join(leafLayer).on(leafLayer.genericLayerId.eq(genericLayer.id))
														.join(layerStyle).on(layerStyle.layerId.eq(leafLayer.id))
														.join(style).on(style.id.eq(layerStyle.styleId))
														.where(genericLayer.identification.eq(layerId)
															.and(style.identification.eq(layerStyleId)))
														.list(
															glId.get(),
															genericLayer.id,
															indexed.getIndex(),
															style.id))
													.execute().<Long>thenApply(result -> {
														if(result != 1) {
															throw new IllegalStateException("Unexpected result count: " + result);
														}
														
														return result;
													});
											}
										})
										.collect(Collectors.toList())).thenCompose(whatever ->
											f.ask(serviceManager, new GetGroupLayer(Optional.of(tx.getTransactionRef()), groupId)))
												.thenCompose(groupLayer -> {
													// TODO put this in a separate query or query all cycles
													if(groupLayer instanceof Failure) {
														Throwable cause = ((Failure) groupLayer).getCause();
														if(cause instanceof CycleException) {
															// get offending group id
															String cycleGroupId = ((CycleException)cause).getLayerId();
															// get group name;
															return tx.query()
															.from(genericLayer)
															.where(genericLayer.identification.eq(cycleGroupId))
															.singleResult(genericLayer.name)
															.thenApply(glName -> {
																if (glName.isPresent()){
																	throw new EnrichedCycleException(
																		(CycleException)cause, 
																		new Response<String>(CrudOperation.UPDATE, 
																			CrudResponse.NOK, glName.get()));
																}else{
																	throw new EnrichedCycleException(
																		(CycleException)cause, 
																		new Response<String>(CrudOperation.UPDATE, 
																			CrudResponse.NOK, cycleGroupId));																	
																}
															});
															
														} else {
															return f.successful(new Response<String>(CrudOperation.UPDATE, 
																CrudResponse.NOK, cause.getMessage()));
														}
													} else {												
														return f.successful(new Response<String>(CrudOperation.UPDATE,
															CrudResponse.OK, groupId));
													}
												});
							});
		})).exceptionally(t -> {
			if(t instanceof CompletionException) {
				t = t.getCause();
			}
			
			if(t instanceof EnrichedCycleException) {
				return ((EnrichedCycleException)t).getResponse();
			}
			
			return new Response<String>(CrudOperation.UPDATE, CrudResponse.NOK, t.getMessage());
		});
	}
	
	private CompletableFuture<Page<LayerGroup>> handleGetGroupParentGroups(GetGroupParentGroups getGroupParentGroups){
		String groupId = getGroupParentGroups.getId();
		
		log.debug ("GetGroupParentGroups: " + groupId);
		final Page.Builder<LayerGroup> builder = new Page.Builder<> ();
		
		final AsyncSQLQuery getGroupsFromLayerStructureQuery =  
			db.query()
			.from(genericLayer)
			.where(genericLayer.id.in(
				new SQLSubQuery()
					.from(layerStructure)
					.join(genericLayer).on(genericLayer.id.eq(layerStructure.childLayerId))
					.leftJoin(service).on(service.genericLayerId.eq(layerStructure.parentLayerId))
					.where(genericLayer.identification.eq(groupId)
						.and(service.genericLayerId.isNull()))
					.list(layerStructure.parentLayerId)
			))
			.orderBy(genericLayer.name.asc());
		
		return getGroupsFromLayerStructureQuery.list(
			genericLayer.identification,
			genericLayer.name
			).thenApply(groups -> {
				for (Tuple group : groups.list()) {
					builder.add(new LayerGroup(
							group.get(genericLayer.identification),
							group.get(genericLayer.name),
							null,
							null,
							null,
							null,
							false,
							false
						));
				}
				return builder.build();
			});
	}
	
	private CompletableFuture<Page<Service>> handleGetGroupParentServices(GetGroupParentServices getGroupParentServices){
		String groupId = getGroupParentServices.getId();
		
		log.debug ("GetGroupParentServices: " + groupId);
		final Page.Builder<Service> builder = new Page.Builder<> ();
		
		final AsyncSQLQuery getGroupsFromLayerStructureQuery =  
			db.query()
			.from(genericLayer)
			.where(genericLayer.id.in(
				new SQLSubQuery()
					.from(layerStructure)
					.join(genericLayer).on(genericLayer.id.eq(layerStructure.childLayerId))
					.leftJoin(service).on(service.genericLayerId.eq(layerStructure.parentLayerId))
					.where(genericLayer.identification.eq(groupId)
						.and(service.genericLayerId.isNotNull()))
					.list(layerStructure.parentLayerId)
			))
			.orderBy(genericLayer.name.asc());
		
		return getGroupsFromLayerStructureQuery.list(
			genericLayer.identification,
			genericLayer.name
			).thenApply(groups -> {
				for (Tuple group : groups.list()) {
					builder.add(new Service(
						group.get(genericLayer.identification),
						group.get(genericLayer.name),
						null,
						null,
						null,
						Collections.emptyList(),
						null, null, null,
						null, null,
						false, false, false
						));
				}
				return builder.build();
			});
	}
	
}
