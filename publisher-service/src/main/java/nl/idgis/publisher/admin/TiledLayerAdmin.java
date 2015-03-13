package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.TiledLayer;
import akka.actor.ActorRef;
import akka.actor.Props;

public class TiledLayerAdmin extends AbstractAdmin {
	
	public TiledLayerAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(TiledLayerAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		doList(TiledLayer.class, this::handleListTiledlayers);
		doGet(TiledLayer.class, this::handleGetTiledlayer);
		doPut(TiledLayer.class, this::handlePutTiledlayer);
		doDelete(TiledLayer.class, this::handleDeleteTiledlayer);
	}

	private CompletableFuture<Page<TiledLayer>> handleListTiledlayers () {
		log.debug ("handleListTiledlayers");
		
		return 
			db.query().from(tiledLayer)
			.join(genericLayer).on(genericLayer.id.eq(tiledLayer.genericLayerId))
			.list(new nl.idgis.publisher.domain.web.QTiledLayer(
					genericLayer.identification,
					genericLayer.name,
					tiledLayer.metaHeight,
					tiledLayer.metaWidth,
					tiledLayer.expireCache,
					tiledLayer.expireClients,
					tiledLayer.gutter,
					null  // mimeformats
					))
			.thenApply(this::toPage);
	}

	
	private CompletableFuture<Optional<TiledLayer>> handleGetTiledlayer (String genericLayerId) {
		log.debug ("handleGetTiledlayer: " + genericLayerId);
		
		return 
			db.query().from(tiledLayer)
			.join(genericLayer).on(genericLayer.id.eq(tiledLayer.genericLayerId))
			.where(genericLayer.identification.eq(genericLayerId))
			.singleResult(new nl.idgis.publisher.domain.web.QTiledLayer(
					genericLayer.identification,
					genericLayer.name,
					tiledLayer.metaHeight,
					tiledLayer.metaWidth,
					tiledLayer.expireCache,
					tiledLayer.expireClients,
					tiledLayer.gutter,
					null  // mimeformats
					));
	}
	
	private CompletableFuture<Response<?>> handlePutTiledlayer(TiledLayer theTiledlayer) {
		String genericLayerId = theTiledlayer.id();
		log.debug ("handle update/create tiledlayer: " + genericLayerId);
		
		return db.transactional(tx ->
			// Check if there is another tiledlayer with the same id
			tx.query().from(tiledLayer)
			.join(genericLayer).on(genericLayer.id.eq(tiledLayer.genericLayerId))
			.where(genericLayer.identification.eq(genericLayerId))
			.singleResult(genericLayer.id)
			.thenCompose(glId -> {
				if (!glId.isPresent()){
					// INSERT
					log.debug("Inserting new tiledlayer with id: " + genericLayerId);
					return tx.insert(tiledLayer)
					.set(tiledLayer.genericLayerId, glId.get())
					.set(tiledLayer.metaHeight, theTiledlayer.metaHeight())
					.set(tiledLayer.metaWidth, theTiledlayer.metaWidth())
					.set(tiledLayer.expireCache, theTiledlayer.expireCache())
					.set(tiledLayer.expireClients, theTiledlayer.expireClients())
					.set(tiledLayer.gutter, theTiledlayer.gutter())
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, genericLayerId));
				} else {
					// UPDATE
					log.debug("Updating tiledlayer with id: " + genericLayerId);
					return tx.update(tiledLayer)
					.set(tiledLayer.metaHeight, theTiledlayer.metaHeight())
					.set(tiledLayer.metaWidth, theTiledlayer.metaWidth())
					.set(tiledLayer.expireCache, theTiledlayer.expireCache())
					.set(tiledLayer.expireClients, theTiledlayer.expireClients())
					.set(tiledLayer.gutter, theTiledlayer.gutter())
					.where(tiledLayer.genericLayerId.eq(glId.get()))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, genericLayerId));
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteTiledlayer(String genericLayerId) {
		log.debug ("handleDeleteTiledlayer: " + genericLayerId);
		return db.transactional(tx -> 
				tx.query().from(tiledLayer)
				.join(genericLayer).on(genericLayer.id.eq(tiledLayer.genericLayerId))
				.where(genericLayer.identification.eq(genericLayerId))
				.singleResult(tiledLayer.id)
				.thenCompose(
				tlId -> {
				return tx.delete(tiledLayer)
					.where(tiledLayer.id.eq(tlId.get()))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.DELETE, CrudResponse.OK, genericLayerId));
		}));

	}
	
	
}
