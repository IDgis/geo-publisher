package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;

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
		addList(TiledLayer.class, this::handleListTiledlayers);
		addGet(TiledLayer.class, this::handleGetTiledlayer);
		addPut(TiledLayer.class, this::handlePutTiledlayer);
		addDelete(TiledLayer.class, this::handleDeleteTiledlayer);
	}

	private CompletableFuture<Page<TiledLayer>> handleListTiledlayers () {
		log.debug ("handleListTiledlayers");
		
		return 
			db.query().from(tiledLayer)
			.list(new nl.idgis.publisher.domain.web.QTiledLayer(
					tiledLayer.identification,
					tiledLayer.name,
					tiledLayer.mimeFormats,
					tiledLayer.metaHeight,
					tiledLayer.metaWidth,
					tiledLayer.expireCache,
					tiledLayer.expireClients,
					tiledLayer.gutter,
					tiledLayer.enabled 
					
					))
			.thenApply(this::toPage);
	}

	
	private CompletableFuture<Optional<TiledLayer>> handleGetTiledlayer (String tiledlayerId) {
		log.debug ("handleGetTiledlayer: " + tiledlayerId);
		
		return 
			db.query().from(tiledLayer)
			.where(tiledLayer.identification.eq(tiledlayerId))
			.singleResult(new nl.idgis.publisher.domain.web.QTiledLayer(
					tiledLayer.identification,
					tiledLayer.name,
					tiledLayer.mimeFormats,
					tiledLayer.metaHeight,
					tiledLayer.metaWidth,
					tiledLayer.expireCache,
					tiledLayer.expireClients,
					tiledLayer.gutter,
					tiledLayer.enabled 
					));
	}
	
	private CompletableFuture<Response<?>> handlePutTiledlayer(TiledLayer theTiledlayer) {
		String tiledlayerId = theTiledlayer.id();
		String tiledlayerName = theTiledlayer.name();
		log.debug ("handle update/create tiledlayer: " + tiledlayerId);
		
		return db.transactional(tx ->
			// Check if there is another tiledlayer with the same name
			tx.query().from(tiledLayer)
			.where(tiledLayer.identification.eq(tiledlayerId))
			.singleResult(tiledLayer.identification)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT
					log.debug("Inserting new tiledlayer with name: " + tiledlayerName);
					return tx.insert(tiledLayer)
					.set(tiledLayer.identification, UUID.randomUUID().toString())
					.set(tiledLayer.name, tiledlayerName)
					.set(tiledLayer.mimeFormats, theTiledlayer.mimeFormats())
					.set(tiledLayer.metaHeight, theTiledlayer.metaHeight())
					.set(tiledLayer.metaWidth, theTiledlayer.metaWidth())
					.set(tiledLayer.expireCache, theTiledlayer.expireCache())
					.set(tiledLayer.expireClients, theTiledlayer.expireClients())
					.set(tiledLayer.gutter, theTiledlayer.gutter())
					.set(tiledLayer.enabled, theTiledlayer.enabled()) 
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, tiledlayerName));
				} else {
					// UPDATE
					log.debug("Updating tiledlayer with name: " + tiledlayerName);
					return tx.update(tiledLayer)
					.set(tiledLayer.mimeFormats, theTiledlayer.mimeFormats())
					.set(tiledLayer.metaHeight, theTiledlayer.metaHeight())
					.set(tiledLayer.metaWidth, theTiledlayer.metaWidth())
					.set(tiledLayer.expireCache, theTiledlayer.expireCache())
					.set(tiledLayer.expireClients, theTiledlayer.expireClients())
					.set(tiledLayer.gutter, theTiledlayer.gutter())
					.set(tiledLayer.enabled, theTiledlayer.enabled()) 
					.where(tiledLayer.identification.eq(tiledlayerId))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, tiledlayerName));
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteTiledlayer(String tiledlayerId) {
		log.debug ("handleDeleteTiledlayer: " + tiledlayerId);
		return db.delete(tiledLayer)
			.where(tiledLayer.identification.eq(tiledlayerId))
			.execute()
			.thenApply(l -> new Response<String>(CrudOperation.DELETE, CrudResponse.OK, tiledlayerId));
	}
	
	
}
