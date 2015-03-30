package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QTiledLayer.tiledLayer;
import static nl.idgis.publisher.database.QTiledLayerMimeformat.tiledLayerMimeformat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import nl.idgis.publisher.database.AsyncHelper;
import nl.idgis.publisher.domain.web.TiledLayer;
import akka.actor.ActorRef;
import akka.event.LoggingAdapter;

public class LayerGroupCommonAdmin extends AbstractAdmin {
	
	public LayerGroupCommonAdmin(ActorRef database) {
		super(database); 
	}

	@Override
	protected void preStartAdmin() {
		// TODO Auto-generated method stub
		
	}

	protected CompletableFuture<List<Long>> insertTiledLayer(AsyncHelper tx, 
			TiledLayer theTiledLayer, Integer genericLayerId, LoggingAdapter log) {
		return tx
			.insert(tiledLayer)
			.set(tiledLayer.metaWidth, theTiledLayer.metaWidth())
			.set(tiledLayer.metaHeight, theTiledLayer.metaHeight())
			.set(tiledLayer.expireCache, theTiledLayer.expireCache())
			.set(tiledLayer.expireClients, theTiledLayer.expireClients())
			.set(tiledLayer.gutter, theTiledLayer.gutter())
			.set(tiledLayer.genericLayerId, genericLayerId)
			.executeWithKey(tiledLayer.id)
			.thenCompose(
				tlId -> {
					log.debug("Inserted tilelayer id: " + tlId);
					log.debug("Insert mimeformats: " + theTiledLayer.mimeformats());
					return f.sequence(
						theTiledLayer.mimeformats().stream()
						    .map(name -> 
						        tx
					            .insert(tiledLayerMimeformat)
					            .set(tiledLayerMimeformat.tiledLayerId, tlId.get()) 
			            		.set(tiledLayerMimeformat.mimeformat, name)
					            .execute())
						    .collect(Collectors.toList()));
			});
	}

}
