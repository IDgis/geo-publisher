package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLeafLayerKeyword.leafLayerKeyword;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.database.QTiledLayerMimeformat.tiledLayerMimeformat;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QStyle.style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.domain.query.GetLayerParentGroups;
import nl.idgis.publisher.domain.query.GetLayerParentServices;
import nl.idgis.publisher.domain.query.ListLayerKeywords;
import nl.idgis.publisher.domain.query.ListLayerStyles;
import nl.idgis.publisher.domain.query.ListLayers;
import nl.idgis.publisher.domain.query.PutLayerKeywords;
import nl.idgis.publisher.domain.query.PutLayerStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.QLayer;
import nl.idgis.publisher.domain.web.Service;
import nl.idgis.publisher.domain.web.TiledLayer;
import nl.idgis.publisher.domain.web.QStyle;
import nl.idgis.publisher.domain.web.Style;
import nl.idgis.publisher.utils.StreamUtils;
import nl.idgis.publisher.utils.TypedList;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.LoggingAdapter;

import com.mysema.query.Tuple;
import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.types.ConstantImpl;
import com.mysema.query.types.ConstructorExpression;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;

public class LayerAdmin extends LayerGroupCommonAdmin {
	
	public LayerAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(LayerAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		doList(Layer.class, this::handleListLayers);
		doGet(Layer.class, this::handleGetLayer);
		doPut(Layer.class, this::handlePutLayer);
		doDelete(Layer.class, this::handleDeleteLayer);
		
		doQuery(ListLayerKeywords.class, this::handleListLayerKeywords);
		doQuery(PutLayerKeywords.class, this::handlePutLayerKeywords);
		
		doQuery(ListLayerStyles.class, this::handleListLayerStyles);
		doQuery(PutLayerStyles.class, this::handlePutLayerStyles);
		
		doQuery (ListLayers.class, this::handleListLayersWithQuery);
		
		doQuery (GetLayerParentGroups.class, this::handleGetLayerParentGroups);
		doQuery (GetLayerParentServices.class, this::handleGetLayerParentServices);
	}

	private CompletableFuture<Page<Layer>> handleListLayers () {
		return handleListLayersWithQuery (new ListLayers (null, null, null));
	}
	
	private CompletableFuture<Page<Layer>> handleListLayersWithQuery (final ListLayers listLayers) {
		final AsyncSQLQuery baseQuery = db
				.query()
				.from(genericLayer)
				.join(leafLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
				.join(dataset).on(leafLayer.datasetId.eq(dataset.id))
				.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))
				.orderBy (genericLayer.name.asc ());
		
		// Add a filter for the query string:
		if (listLayers.getQuery () != null) {
			baseQuery.where (
					genericLayer.name.containsIgnoreCase (listLayers.getQuery ())
					.or (genericLayer.title.containsIgnoreCase (listLayers.getQuery()))
				);
		}
		
		// Add a filter for the published flag:
		if (listLayers.getPublished () != null) {
			baseQuery.where (genericLayer.published.eq (listLayers.getPublished ()));
		}
		
		// Add a filter for the dataset id:
		if (listLayers.getDatasetId () != null) {
			baseQuery.join (dataset).on (dataset.id.eq (leafLayer.datasetId))
				.where (dataset.identification.eq (listLayers.getDatasetId ()));
		}
		
		final AsyncSQLQuery listQuery = baseQuery.clone ();
		
		singlePage (listQuery, listLayers.getPage ());
		
		return baseQuery
			.count ()
			.thenCompose ((count) -> {
				final Page.Builder<Layer> builder = new Page.Builder<> ();
				
				addPageInfo (builder, listLayers.getPage (), count);
				
				return listQuery
					.list (
							genericLayer.identification,
							genericLayer.name,
							genericLayer.title,
							genericLayer.abstractCol,
							genericLayer.published,
							dataset.identification,
							dataset.name,
							tiledLayer.genericLayerId
							)
					.thenApply ((layers) -> {
						for (Tuple layer : layers.list()) {
							boolean hasTiledLayer = layer.get(tiledLayer.genericLayerId) != null;
							log.debug("Layer: " + layer.get(genericLayer.name) + ", tiling = " + hasTiledLayer);
							builder.add(new Layer(
									layer.get(genericLayer.identification),
									layer.get(genericLayer.name),
									layer.get(genericLayer.title),
									layer.get(genericLayer.abstractCol),
									layer.get(genericLayer.published),
									layer.get(dataset.identification),
									layer.get(dataset.name),
									(hasTiledLayer
										? new TiledLayer(
											layer.get(genericLayer.identification),
											layer.get(genericLayer.name),
											layer.get(null),
											layer.get(null),
											layer.get(null),
											layer.get(null),
											layer.get(null),
											null)
										: null),
									null, null));
						}
						return builder.build ();
					});
			});
	}
	
	private CompletableFuture<Optional<Layer>> handleGetLayer (String layerId) {
		log.debug("handleGetLayer: " + layerId);
		
		List<Path<?>> layerColumns = new ArrayList<>();
		layerColumns.addAll(Arrays.asList(genericLayer.all()));
		layerColumns.addAll(Arrays.asList(tiledLayer.all()));
		layerColumns.addAll(Arrays.asList(leafLayer.all()));
		layerColumns.addAll(Arrays.asList(dataset.all()));

		return db.transactional(tx -> 
			tx.query()
			.from(genericLayer)
			.join(leafLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
			.join(dataset).on(leafLayer.datasetId.eq(dataset.id))
			.leftJoin(tiledLayer).on(tiledLayer.genericLayerId.eq(genericLayer.id))
			.where(genericLayer.identification.eq(layerId))
			.singleResult(layerColumns.toArray (new Path<?>[layerColumns.size ()])).thenCompose(optionalLayer -> {
				if(optionalLayer.isPresent()) {
					Tuple layer = optionalLayer.get();					
					log.debug("generic layer id: " + layer.get(genericLayer.id));
					return tx.query()
						.from(leafLayerKeyword)
						.where(leafLayerKeyword.leafLayerId.eq(layer.get(leafLayer.id)))
						.list(leafLayerKeyword.keyword).thenCompose(keywords -> {
							log.debug("tiled layer   id: " + layer.get(tiledLayer.id));
							boolean hasTiledLayer = layer.get(tiledLayer.genericLayerId) != null;
							log.debug("tiled layer glId: " + layer.get(tiledLayer.genericLayerId) + " = " + hasTiledLayer);
							
							CompletableFuture<TypedList<String>> mimeformatsQuery;
							if(hasTiledLayer) {
								mimeformatsQuery = tx.query()
								.from(tiledLayerMimeformat)
								.where(tiledLayerMimeformat.tiledLayerId.eq(layer.get(tiledLayer.id)))
								.list(tiledLayerMimeformat.mimeformat);
							} else {
								mimeformatsQuery = f.successful(new TypedList<>(String.class, Collections.emptyList()));
							}
							
							return mimeformatsQuery.thenCompose(mimeFormats ->									
								tx.query()
								.from(genericLayer)
								.join(leafLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
								.join(layerStyle).on(layerStyle.layerId.eq(leafLayer.id))
								.join(style).on(layerStyle.styleId.eq(style.id))
								.where(genericLayer.identification.eq(layerId))
								.list(new QStyle(style.identification, style.name, style.definition,style.styleType, ConstantImpl.create(true)))
								.thenApply(styles ->
									Optional.of(new Layer(
										layer.get(genericLayer.identification),
										layer.get(genericLayer.name),
										layer.get(genericLayer.title),
										layer.get(genericLayer.abstractCol),
										layer.get(genericLayer.published),
										layer.get(dataset.identification),
										layer.get(dataset.name),
											(hasTiledLayer
												? new TiledLayer(
													layer.get(genericLayer.identification),
													layer.get(genericLayer.name),
													layer.get(tiledLayer.metaWidth),
													layer.get(tiledLayer.metaHeight),
													layer.get(tiledLayer.expireCache),
													layer.get(tiledLayer.expireClients),
													layer.get(tiledLayer.gutter),
													mimeFormats.list())
												: null)
										,
										keywords.list(), styles.list() ))));
						});
				} else {
					return f.successful(Optional.empty());
				}
			}));
	}
	
	private CompletableFuture<Response<?>> handlePutLayer(Layer theLayer) {
		String layerId = theLayer.id();
		String layerName = theLayer.name();
		String datasetId = theLayer.datasetId();
		log.debug("handle update/create layer: " + layerId);

		return db
			.transactional(tx ->
			// Check if there is another layer with the same id
			tx.query()
				.from(genericLayer)
				.where(genericLayer.identification.eq(layerId))
				.singleResult(genericLayer.id)
				.thenCompose(msg -> {
					if (!msg.isPresent()) {
						// INSERT
						String newLayerId = UUID.randomUUID().toString();
						log.debug("Inserting new generic_layer with name: " + layerName + ", ident: "
								+ newLayerId);
						return tx
							.insert(genericLayer)
							.set(genericLayer.identification, newLayerId)
							.set(genericLayer.name, theLayer.name())
							.set(genericLayer.title, theLayer.title())
							.set(genericLayer.abstractCol, theLayer.abstractText())
							.set(genericLayer.published, theLayer.published())
							.execute()
							.thenCompose(
								gl -> {
									log.debug("Inserted generic_layer: #" + gl);
									return tx
										.query()
										.from(genericLayer)
										.where(genericLayer.identification.eq(newLayerId))
										.singleResult(genericLayer.id)
										.thenCompose(
											glId -> {
												log.debug("Finding dataset identification: " + datasetId);
												return tx
													.query()
													.from(dataset)
													.where(dataset.identification.eq(datasetId))
													.singleResult(dataset.id)
													.thenCompose(
														dsId -> {
														log.debug("dataset found id:  "
																	+ dsId.get());
														log.debug("Inserting new leaf_layer with generic_layer id: "
																+ glId.get());
													return tx
														.insert(leafLayer)
														.set(leafLayer.genericLayerId, glId.get())
														.set(leafLayer.datasetId, dsId.get())
														.execute()
														.thenCompose(
															n -> {
																if (theLayer.tiledLayer().isPresent()){
																	log.debug("Insert tiledlayer ");
																	return insertTiledLayer(tx, theLayer.tiledLayer().get(), glId.get(), log)
																			.thenApply(whatever ->
																	        	new Response<String>(CrudOperation.CREATE,
																	                CrudResponse.OK, newLayerId));
																} else {
																	return f.successful( 
																		new Response<String>(CrudOperation.CREATE,
															            CrudResponse.OK, newLayerId));
																}
															});
														});
											});
									});
					} else {
						// UPDATE
						log.debug("genericlayer id: " + msg.get());
						log.debug("Updating layer with name: " + layerName);
						return tx
							.update(genericLayer)
							.set(genericLayer.name, theLayer.name())
							.set(genericLayer.title, theLayer.title())
							.set(genericLayer.abstractCol, theLayer.abstractText())
							.set(genericLayer.published, theLayer.published())
							.where(genericLayer.identification.eq(layerId))
							.execute()
							.thenCompose(
								gl -> {
									log.debug("updated generic_layer: #" + gl);
									return tx
										.query()
										.from(genericLayer)
										.where(genericLayer.identification.eq(layerId))
										.singleResult(genericLayer.id)
										.thenCompose(
											glId -> {
												return tx
													.delete(tiledLayer)
													.where(tiledLayer.genericLayerId.eq(glId.get()))
													.execute()
													.thenCompose(
														tlIdOld -> {
														log.debug("Deleted tiledlayer glId: " + glId.get());
														if (theLayer.tiledLayer().isPresent()){
															return insertTiledLayer(tx, theLayer.tiledLayer().get(), glId.get(), log)
															    .thenApply(whatever ->
															        new Response<String>(CrudOperation.UPDATE,
													                CrudResponse.OK, layerId));
														} else {
															return f.successful( 
																new Response<String>(CrudOperation.UPDATE,
													            CrudResponse.OK, layerId));
														}
												});
											});
									});
						}
					}));
	}

	private CompletableFuture<Response<?>> handleDeleteLayer(String layerId) {
		log.debug("handleDeleteLayer: " + layerId);

		return db.transactional(tx -> tx
			.query()
			.from(genericLayer)
			.where(genericLayer.identification.eq(layerId))
			.singleResult(genericLayer.id)
			.thenCompose(
				glId -> {
				log.debug("genericlayer id: " + glId.get());
					return tx.query().
						from(leafLayer)
						.where(leafLayer.genericLayerId.eq(glId.get()))
						.singleResult(leafLayer.id)
						.thenCompose(
							llId -> {
							log.debug("delete layerstyles with leaflayer id" + llId.get());
							return tx
								.delete(layerStyle)
								.where(layerStyle.layerId.eq(llId.get()))
								.execute()
								.thenCompose(
									ls -> {
									log.debug("delete leaflayer " + llId.get());
									return tx
										.delete(leafLayer)
										.where(leafLayer.genericLayerId.eq(glId.get()))
										.execute()
										.thenCompose(
											ll -> {
												// remove from layerStructure if present in parent or child
												log.debug("delete layerstructures " + glId.get());
												return tx
													.delete(layerStructure)
													.where(layerStructure.parentLayerId.eq(glId.get()).or(
														   layerStructure.childLayerId.eq(glId.get())))
													.execute()
													.thenCompose(
														nr -> {
														log.debug("LayerStructures deleted: #" + nr);
														log.debug("delete genericlayer " + glId.get());
														return tx
															.delete(genericLayer)
															.where(genericLayer.id.eq(glId.get()))
															.execute()
															.thenApply(
																l -> new Response<String>(CrudOperation.DELETE,
																CrudResponse.OK, layerId));
														});
											});
									});
							});
				}));
	}
	
	private CompletableFuture<List<String>> handleListLayerKeywords (final ListLayerKeywords listLayerKeywords) {
		String layerId = listLayerKeywords.layerId();
		log.debug("listLayerKeywords layerId: " + layerId);
		return db.transactional(tx -> tx
			.query()
			.from(genericLayer)
			.where(genericLayer.identification.eq(layerId))
			.singleResult(genericLayer.id)
			.thenCompose(
				glId -> {
				log.debug("genericlayer id: " + glId.get());
				return tx
					.query()
					.from(leafLayer, leafLayerKeyword)
					.where(leafLayer.genericLayerId.eq(glId.get()).and(leafLayerKeyword.leafLayerId.eq(leafLayer.id)))
					.list(leafLayerKeyword.keyword).thenApply(resp -> resp.list());
		}));
	}
	
	private CompletableFuture<Response<?>> handlePutLayerKeywords (final PutLayerKeywords putLayerKeywords) {
		String layerId = putLayerKeywords.layerId();
		List<String> layerKeywords =  putLayerKeywords.keywordList();
		log.debug("handlePutLayerKeywords layerId: " + layerId + ", keywords: " +layerKeywords);
		return db.transactional(tx -> tx
			.query()
			.from(genericLayer)
			.where(genericLayer.identification.eq(layerId))
			.singleResult(genericLayer.id)
			.thenCompose(
				glId -> {
				log.debug("genericlayer id: " + glId.get());
				return tx.query().
					from(leafLayer)
					.where(leafLayer.genericLayerId.eq(glId.get()))
					.singleResult(leafLayer.id)
					.thenCompose(
						llId -> {
							// A. delete the existing keywords of this layer
							return tx.delete(leafLayerKeyword)
								.where(leafLayerKeyword.leafLayerId.eq(llId.get()))
								.execute()
								.thenCompose(
									llNr -> {
										// B. insert items of layerKeywords	
										
										return f.sequence(
												layerKeywords.stream()
												    .map(name -> 
												        tx
												            .insert(leafLayerKeyword)
												            .set(leafLayerKeyword.leafLayerId,llId.get()) 
										            		.set(leafLayerKeyword.keyword, name)
												            .execute())
												    .collect(Collectors.toList())).thenApply(whatever ->
												        new Response<String>(CrudOperation.CREATE,
												                CrudResponse.OK, layerId));
									});
						});
		}));
	}
	
	private CompletableFuture<List<Style>> handleListLayerStyles (final ListLayerStyles listLayerStyles) {
		String layerId = listLayerStyles.layerId();
		log.debug("handleListLayerStyles layerId: " + layerId);
		return db.transactional(tx -> tx
			.query()
			.from(genericLayer)
			.where(genericLayer.identification.eq(layerId))
			.singleResult(genericLayer.id)
			.thenCompose(
				glId -> {
				log.debug("genericlayer id: " + glId.get());
				return tx
					.query()
					.from(style, leafLayer, layerStyle)
					//.join(leafLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
					.where(leafLayer.genericLayerId.eq(glId.get()).and(layerStyle.layerId.eq(leafLayer.id))
							.and(layerStyle.styleId.eq(style.id)))
					.list(new QStyle(style.identification, style.name, style.definition,style.styleType, ConstantImpl.create(true)))
					.thenApply(this::toList);
		}));
	}
	
	private CompletableFuture<Response<?>> handlePutLayerStyles (final PutLayerStyles putLayerStyles) {
		String layerId = putLayerStyles.layerId();
		List<String> layerStyles =  putLayerStyles.styleIdList();
		log.debug("handlePutLayerStyles layerId: " + layerId + ", styles: " +layerStyles);
		return db.transactional(tx -> tx
			.query()
			.from(genericLayer)
			.where(genericLayer.identification.eq(layerId))
			.singleResult(genericLayer.id)
			.thenCompose(
				glId -> {
				log.debug("genericlayer id: " + glId.get());
				return tx.query().
					from(leafLayer)
					.where(leafLayer.genericLayerId.eq(glId.get()))
					.singleResult(leafLayer.id)
					.thenCompose(
						llId -> {
							// A. delete the existing styles of this layer
							return tx.delete(layerStyle)
								.where(layerStyle.layerId.eq(llId.get()))
								.execute()
								.thenCompose(
									llNr -> {
										// B. insert items of layerStyles	
										return f.sequence(
												StreamUtils.index(
													layerStyles.stream()
												)
											    .map(indexed -> tx
											.insert(layerStyle)
											.columns(
												layerStyle.layerId, 
												layerStyle.styleId,
												layerStyle.defaultStyle)
											.select(new SQLSubQuery().from(style)
												.where(style.identification.eq(indexed.getValue()))
												.list(
													llId.get(),
													style.id,
													indexed.getIndex()==0)
											)
											.execute())
											.collect(Collectors.toList())).thenApply(whatever ->
											new Response<String>(CrudOperation.UPDATE,
													CrudResponse.OK, layerId));
									});
						});
		}));
	}
	
	private CompletableFuture<Page<LayerGroup>> handleGetLayerParentGroups(GetLayerParentGroups getLayerParentGroups){
		String layerId = getLayerParentGroups.getId();
		
		log.debug ("handleGetLayerParentGroups: " + layerId);
		final Page.Builder<LayerGroup> builder = new Page.Builder<> ();
		
		final AsyncSQLQuery getGroupsFromLayerStructureQuery =  
			db.query()
			.from(genericLayer)
			.where(genericLayer.id.in(
				new SQLSubQuery()
					.from(layerStructure)
					.join(genericLayer).on(genericLayer.id.eq(layerStructure.childLayerId))
					.join(leafLayer).on(leafLayer.genericLayerId.eq(genericLayer.id))
					.leftJoin(service).on(service.genericLayerId.eq(layerStructure.parentLayerId))
					.where(genericLayer.identification.eq(layerId)
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
						null
						));
				}
				return builder.build();
			});
	}

	private CompletableFuture<Page<Service>> handleGetLayerParentServices(GetLayerParentServices getLayerParentServices){
		String layerId = getLayerParentServices.getId();
		
		log.debug ("handleGetLayerParentServices: " + layerId);
		final Page.Builder<Service> builder = new Page.Builder<> ();
		
		final AsyncSQLQuery getGroupsFromLayerStructureQuery =  
			db.query()
			.from(genericLayer)
			.where(genericLayer.id.in(
				new SQLSubQuery()
					.from(layerStructure)
					.join(genericLayer).on(genericLayer.id.eq(layerStructure.childLayerId))
					.join(leafLayer).on(leafLayer.genericLayerId.eq(genericLayer.id))
					.leftJoin(service).on(service.genericLayerId.eq(layerStructure.parentLayerId))
					.where(genericLayer.identification.eq(layerId)
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
						null, null, null, null
						));
				}
				return builder.build();
			});
	}
}
