package nl.idgis.publisher.admin;

import com.mysema.query.types.ConstantImpl;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.QLayerGroup;
import nl.idgis.publisher.domain.web.LayerGroup;
import akka.actor.ActorRef;
import akka.actor.Props;

public class LayerGroupAdmin extends AbstractAdmin {
	
	public LayerGroupAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(LayerGroupAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		addList(LayerGroup.class, this::handleListLayergroups);
		addGet(LayerGroup.class, this::handleGetLayergroup);
		addPut(LayerGroup.class, this::handlePutLayergroup);
		addDelete(LayerGroup.class, this::handleDeleteLayergroup);
	}

	private CompletableFuture<Page<LayerGroup>> handleListLayergroups () {
		log.debug ("handleListLayergroups");
		
		return 
			db.query().from(genericLayer)
			.list(new QLayerGroup(
					genericLayer.identification,
					genericLayer.name,
					genericLayer.title, 
					genericLayer.abstractCol,
					ConstantImpl.create(false)
				))
			.thenApply(this::toPage);
	}

	
	private CompletableFuture<Optional<LayerGroup>> handleGetLayergroup (String layergroupId) {
		log.debug ("handleGetLayergroup: " + layergroupId);
		
		return 
			db.query().from(genericLayer)
			.where(genericLayer.identification.eq(layergroupId))
			.singleResult(new QLayerGroup(
					genericLayer.identification,
					genericLayer.name,
					genericLayer.title, 
					genericLayer.abstractCol,
					ConstantImpl.create(false)
			));		
	}
	
	private CompletableFuture<Response<?>> handlePutLayergroup(LayerGroup theLayergroup) {
		String layergroupId = theLayergroup.id();
		String layergroupName = theLayergroup.name();
		log.debug ("handle update/create layergroup: " + layergroupId);
		
		return db.transactional(tx ->
			// Check if there is another layergroup with the same name
			tx.query().from(genericLayer)
			.where(genericLayer.name.eq(layergroupId))
			.singleResult(genericLayer.name)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT
					log.debug("Inserting new layergroup with name: " + layergroupName);
					return tx.insert(genericLayer)
					.set(genericLayer.identification, UUID.randomUUID().toString())
					.set(genericLayer.name, layergroupName)
					.set(genericLayer.title, theLayergroup.title())
					.set(genericLayer.abstractCol, theLayergroup.abstractText())
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, layergroupName));
				} else {
					// UPDATE
					log.debug("Updating layergroup with name: " + layergroupName);
					return tx.update(genericLayer)
							.set(genericLayer.title, theLayergroup.title())
							.set(genericLayer.abstractCol, theLayergroup.abstractText())
					.where(genericLayer.identification.eq(layergroupId))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, layergroupName));
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteLayergroup(String layergroupId) {
		log.debug ("handleDeleteLayergroup: " + layergroupId);
		return db.delete(genericLayer)
			.where(genericLayer.identification.eq(layergroupId))
			.execute()
			.thenApply(l -> new Response<String>(CrudOperation.DELETE, CrudResponse.OK, layergroupId));
	}
	
}
