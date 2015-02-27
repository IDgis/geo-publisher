package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QStyle.style;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.domain.query.ListLayerStyles;
import nl.idgis.publisher.domain.query.ListLayers;
import nl.idgis.publisher.domain.query.PutLayerStyles;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.QLayer;
import nl.idgis.publisher.domain.web.QStyle;
import nl.idgis.publisher.domain.web.Style;
import akka.actor.ActorRef;
import akka.actor.Props;

import com.mysema.query.sql.SQLSubQuery;

public class LayerAdmin extends AbstractAdmin {
	
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
		
		doQuery(ListLayerStyles.class, this::handleListLayerStyles);
		doQuery(PutLayerStyles.class, this::handlePutLayerStyles);
		
		doQuery (ListLayers.class, this::handleListLayersWithQuery);
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
		
		final AsyncSQLQuery listQuery = baseQuery.clone ();
		
		singlePage (listQuery, listLayers.getPage ());
		
		return baseQuery
				.count ()
				.thenCompose ((count) -> {
					final Page.Builder<Layer> builder = new Page.Builder<> ();
					
					addPageInfo (builder, listLayers.getPage (), count);
					
					return listQuery
						.list (new QLayer(
								genericLayer.identification, 
								genericLayer.name, 
								genericLayer.title,
								genericLayer.abstractCol, 
								genericLayer.published, 
								dataset.identification
							))
						.thenApply ((styles) -> {
							builder.addAll (styles.list ());
							return builder.build ();
						});
				});
	}
	
	private CompletableFuture<Optional<Layer>> handleGetLayer (String layerId) {
		log.debug("handleGetLayer: " + layerId);

		return db
			.query()
			.from(genericLayer)
			.join(leafLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
			.join(dataset).on(leafLayer.datasetId.eq(dataset.id))
			.where(genericLayer.identification.eq(layerId))
			.singleResult(new QLayer(genericLayer.identification, genericLayer.name, genericLayer.title,
				genericLayer.abstractCol, genericLayer.published, dataset.identification));
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
														.thenApply(
															l -> new Response<String>(
																CrudOperation.CREATE,
																CrudResponse.OK,
																newLayerId));
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
										/* update of leaflayer is useful when filter and metadata fields are used
										.thenCompose(
											glId -> {
												log.debug("Updating leaf_layer with generic_layer id: " + glId.get());
												return tx
													.update(leafLayer)
													.where(leafLayer.genericLayerId.eq(glId.get()))
													.execute()
										*/
													.thenApply(
														l ->  new Response<String>(
																CrudOperation.UPDATE,
																CrudResponse.OK,
																layerId));
//										});
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
					.list(new QStyle(style.identification, style.name, style.definition,style.styleType))
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
										return tx
											.insert(layerStyle)
											.columns(
												layerStyle.layerId, 
												layerStyle.styleId)
											.select(new SQLSubQuery().from(style)
												.where(style.identification.in(layerStyles))
												.list(
													llId.get(),
													style.id))
											.execute().thenApply(whatever ->
											new Response<String>(CrudOperation.UPDATE,
													CrudResponse.OK, layerId));
									});
						});
		}));
	}
	
}
