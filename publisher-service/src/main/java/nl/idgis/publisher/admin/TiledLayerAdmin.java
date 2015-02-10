package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QTiledlayer.tiledlayer;

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
			db.query().from(tiledlayer)
			.list(new nl.idgis.publisher.domain.web.QTiledLayer(
					tiledlayer.identification,
					tiledlayer.name,
					tiledlayer.mimeformats,
					tiledlayer.metaheight,
					tiledlayer.metawidth,
					tiledlayer.expirecache,
					tiledlayer.expireclients,
					tiledlayer.gutter,
					tiledlayer.enabled 
					
					))
			.thenApply(this::toPage);
	}

	
	private CompletableFuture<Optional<TiledLayer>> handleGetTiledlayer (String tiledlayerId) {
		log.debug ("handleGetTiledlayer: " + tiledlayerId);
		
		return 
			db.query().from(tiledlayer)
			.where(tiledlayer.identification.eq(tiledlayerId))
			.singleResult(new nl.idgis.publisher.domain.web.QTiledLayer(
					tiledlayer.identification,
					tiledlayer.name,
					tiledlayer.mimeformats,
					tiledlayer.metaheight,
					tiledlayer.metawidth,
					tiledlayer.expirecache,
					tiledlayer.expireclients,
					tiledlayer.gutter,
					tiledlayer.enabled 
					));
	}
	
	private CompletableFuture<Response<?>> handlePutTiledlayer(TiledLayer theTiledlayer) {
		String tiledlayerId = theTiledlayer.id();
		String tiledlayerName = theTiledlayer.name();
		log.debug ("handle update/create tiledlayer: " + tiledlayerId);
		
		return db.transactional(tx ->
			// Check if there is another tiledlayer with the same name
			tx.query().from(tiledlayer)
			.where(tiledlayer.name.eq(tiledlayerId))
			.singleResult(tiledlayer.name)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT
					log.debug("Inserting new tiledlayer with name: " + tiledlayerName);
					return tx.insert(tiledlayer)
					.set(tiledlayer.identification, UUID.randomUUID().toString())
					.set(tiledlayer.name, tiledlayerName)
					.set(tiledlayer.mimeformats, theTiledlayer.mimeFormats())
					.set(tiledlayer.metaheight, theTiledlayer.metaHeight())
					.set(tiledlayer.metawidth, theTiledlayer.metaWidth())
					.set(tiledlayer.expirecache, theTiledlayer.expireCache())
					.set(tiledlayer.expireclients, theTiledlayer.expireClients())
					.set(tiledlayer.gutter, theTiledlayer.gutter())
					.set(tiledlayer.enabled, theTiledlayer.enabled()) 
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, tiledlayerName));
				} else {
					// UPDATE
					log.debug("Updating tiledlayer with name: " + tiledlayerName);
					return tx.update(tiledlayer)
					.set(tiledlayer.mimeformats, theTiledlayer.mimeFormats())
					.set(tiledlayer.metaheight, theTiledlayer.metaHeight())
					.set(tiledlayer.metawidth, theTiledlayer.metaWidth())
					.set(tiledlayer.expirecache, theTiledlayer.expireCache())
					.set(tiledlayer.expireclients, theTiledlayer.expireClients())
					.set(tiledlayer.gutter, theTiledlayer.gutter())
					.set(tiledlayer.enabled, theTiledlayer.enabled()) 
					.where(tiledlayer.identification.eq(tiledlayerId))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, tiledlayerName));
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteTiledlayer(String tiledlayerId) {
		log.debug ("handleDeleteTiledlayer: " + tiledlayerId);
		return db.delete(tiledlayer)
			.where(tiledlayer.identification.eq(tiledlayerId))
			.execute()
			.thenApply(l -> new Response<String>(CrudOperation.DELETE, CrudResponse.OK, tiledlayerId));
	}
	
	
}
