package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QLeafLayer.leafLayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.types.ConstantImpl;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Layer;
import nl.idgis.publisher.domain.web.QLayer;
import akka.actor.ActorRef;
import akka.actor.Props;

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
	}

	private CompletableFuture<Page<Layer>> handleListLayers () {
		log.debug ("handleListLayers");
		return 
			db.query().from(leafLayer)
			.list(new QLayer(
					leafLayer.identification,
					leafLayer.name,
					leafLayer.title, 
					leafLayer.abstractCol,
					leafLayer.keywords,
					ConstantImpl.create(false)
				))
			.thenApply(this::toPage);
	}

	
	private CompletableFuture<Optional<Layer>> handleGetLayer (String layerId) {
		log.debug ("handleGetLayer: " + layerId);
		
		return 
			db.query().from(leafLayer)
			.where(leafLayer.identification.eq(layerId))
			.singleResult(new QLayer(
					leafLayer.identification,
					leafLayer.name,
					leafLayer.title, 
					leafLayer.abstractCol,
					leafLayer.keywords,
					ConstantImpl.create(false)
			));		
	}
	
	private CompletableFuture<Response<?>> handlePutLayer(Layer theLayer) {
		String layerId = theLayer.id();
		String layerName = theLayer.name();
		log.debug ("handle update/create layer: " + layerId);
		
		return db.transactional(tx ->
			// Check if there is another layer with the same name
			tx.query().from(leafLayer)
			.where(leafLayer.name.eq(layerId))
			.singleResult(leafLayer.name)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT
					log.debug("Inserting new layer with name: " + layerName);
					return tx.insert(leafLayer)
					.set(leafLayer.identification, UUID.randomUUID().toString())
					.set(leafLayer.name, layerName)
					.set(leafLayer.title, theLayer.title())
					.set(leafLayer.abstractCol, theLayer.abstractText())
					.set(leafLayer.keywords, theLayer.keywords())
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, layerName));
				} else {
					// UPDATE
					log.debug("Updating layer with name: " + layerName);
					return tx.update(leafLayer)
					.set(leafLayer.title, theLayer.title())
					.set(leafLayer.abstractCol, theLayer.abstractText())
					.set(leafLayer.keywords, theLayer.keywords())
					.where(leafLayer.identification.eq(layerId))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, layerName));
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteLayer(String layerId) {
		log.debug ("handleDeleteLayer: " + layerId);
		return db.delete(leafLayer)
			.where(leafLayer.identification.eq(layerId))
			.execute()
			.thenApply(l -> new Response<String>(CrudOperation.DELETE, CrudResponse.OK, layerId));
	}
	

}
