package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QStyle.style;
import static nl.idgis.publisher.database.QLayerStyle.layerStyle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.domain.query.DeleteLayerStyle;
import nl.idgis.publisher.domain.query.ListLayerStyles;
import nl.idgis.publisher.domain.query.PutLayerStyle;
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
import com.mysema.query.types.ConstantImpl;

public class LayerAdmin extends AbstractAdmin {
	
	public LayerAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(LayerAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		addList(Layer.class, this::handleListLayers);
		addGet(Layer.class, this::handleGetLayer);
		addPut(Layer.class, this::handlePutLayer);
		addDelete(Layer.class, this::handleDeleteLayer);
		
		addQuery(ListLayerStyles.class, this::handleListLayerStyles);
	}

	private CompletableFuture<Page<Layer>> handleListLayers () {
		log.debug("handleListLayers");
		return db
				.query()
				.from(genericLayer)
				.join(leafLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
				.list(new QLayer(genericLayer.identification, genericLayer.name, genericLayer.title,
						genericLayer.abstractCol, leafLayer.keywords, genericLayer.published))
				.thenApply(this::toPage);
	}

	
	private CompletableFuture<Optional<Layer>> handleGetLayer (String layerId) {
		log.debug("handleGetLayer: " + layerId);

		return db
				.query()
				.from(genericLayer)
				.join(leafLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
				.where(genericLayer.identification.eq(layerId))
				.singleResult(new QLayer(genericLayer.identification, genericLayer.name, genericLayer.title,
							genericLayer.abstractCol, leafLayer.keywords, genericLayer.published));
	}
	
	private CompletableFuture<Response<?>> handlePutLayer(Layer theLayer) {
		String layerId = theLayer.id();
		String layerName = theLayer.name();
		log.debug("handle update/create layer: " + layerId);

		return db
				.transactional(tx ->
				// Check if there is another layer with the same name
				tx.query()
						.from(genericLayer)
						.where(genericLayer.identification.eq(layerId))
						.singleResult(genericLayer.id)
						.thenCompose(msg -> {
							log.debug("genericlayer id: " + msg.get().toString());
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
													log.debug("Inserted generic_layer #: " + gl);
													return tx
															.query()
															.from(genericLayer)
															.where(genericLayer.identification.eq(newLayerId))
															.singleResult(genericLayer.id)
															.thenCompose(
																	glId -> {
																		log.debug("Inserting new leaf_layer with generic_layer id: "
																				+ glId.get());
																		return tx
																				.insert(leafLayer)
																				.set(leafLayer.genericLayerId,
																						glId.get())
																				.set(leafLayer.keywords,
																						theLayer.keywords())
																				.execute()
																				.thenApply(
																						l -> new Response<String>(
																								CrudOperation.CREATE,
																								CrudResponse.OK,
																								layerName));
																	});
												});
							} else {
								// UPDATE
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
													log.debug("updated generic_layer #: " + gl);
													return tx
															.query()
															.from(genericLayer)
															.where(genericLayer.identification.eq(layerId))
															.singleResult(genericLayer.id)
															.thenCompose(
																	glId -> {
																		log.debug("Updating new leaf_layer with generic_layer id: "
																				+ glId.get());
																		return tx
																				.update(leafLayer)
																				.set(leafLayer.keywords,
																						theLayer.keywords())
																				.where(leafLayer.genericLayerId.eq(glId
																						.get()))
																				.execute()
																				.thenApply(
																						l -> new Response<String>(
																								CrudOperation.UPDATE,
																								CrudResponse.OK,
																								layerName));
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
									log.debug("delete leaflayer " + glId.get());
									return tx
										.delete(leafLayer)
										.where(leafLayer.genericLayerId.eq(glId.get()))
										.execute()
										.thenCompose(
											ll -> {
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
						}));
	}
	
	private CompletableFuture<List<Style>> handleListLayerStyles (final ListLayerStyles listLayerStyles) {
		log.debug("handleListLayerStyles");
		String layerId = listLayerStyles.layerId();
		return db
				.query()
				.from(style, leafLayer, layerStyle)
				.where(leafLayer.identification.eq(layerId).and(layerStyle.layerId.eq(leafLayer.id))
						.and(layerStyle.styleId.eq(style.id)))
				.list(new QStyle(style.identification, style.name, style.format, style.version, style.definition))
				.thenApply(this::toList);
		//		return db.query().from(style, leafLayer)
//		.join(style).on(layerStyle.styleId.eq(style.id))
//		.join(leafLayer).on(layerStyle.layerId.eq(leafLayer.id))
//		.where(leafLayer.identification.eq(layerId))
//		.list(new QStyle(style.identification,style.name,style.format, style.version, style.definition))
//		.thenApply(this::toList);

	}
	
//	private CompletableFuture<Response<?>> handlePutLayerStyle (final PutLayerStyle putLayerStyle) {
//		log.debug ("handleDeleteLayerStyle: " );
//		String layerId = putLayerStyle.layerId();
//		String styleId = putLayerStyle.styleId();
//		
//		return db.transactional(tx ->
//			tx.query().from(leafLayer)
//			.where(leafLayer.identification.eq(layerId))
//			.singleResult(leafLayer.id)
//			.thenCompose(msg -> {
//				if (!msg.isPresent()){
//					return tx.delete(layerStyle)
//						.where(layerStyle.layerId.eq(msg.get()))
//						.execute()
//						.thenCompose(dsl -> {
//							return tx.insert(layerStyle)
//								.set(layerStyle.layerId, msg.get())
//								.set(layerStyle.styleId, 
//										new SQLSubQuery().from(style).where(style.identification.eq(styleId)).unique(style.id))
//								.execute()
//								.thenApply(isl -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, isl.toString()));
//						});
//				}
//		}));
//	}

}