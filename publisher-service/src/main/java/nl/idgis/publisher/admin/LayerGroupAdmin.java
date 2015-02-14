package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QLayerStructure.layerStructure;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.domain.query.GetGroupStructure;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.LayerGroup;
import nl.idgis.publisher.domain.web.NotFound;
import nl.idgis.publisher.domain.web.QLayerGroup;
import nl.idgis.publisher.domain.web.tree.GroupLayer;
import nl.idgis.publisher.service.manager.messages.GetGroupLayer;
import akka.actor.ActorRef;
import akka.actor.Props;

public class LayerGroupAdmin extends AbstractAdmin {
	
	private final ActorRef serviceManager;
	
	public LayerGroupAdmin(ActorRef database, ActorRef serviceManager) {
		super(database); 
		
		this.serviceManager = serviceManager;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager) {
		return Props.create(LayerGroupAdmin.class, database, serviceManager);
	}

	@Override
	protected void preStartAdmin() {
		doList(LayerGroup.class, this::handleListLayergroups);
		doGet(LayerGroup.class, this::handleGetLayergroup);
		doPut(LayerGroup.class, this::handlePutLayergroup);
		doDelete(LayerGroup.class, this::handleDeleteLayergroup);
		
		doQueryOptional(GetGroupStructure.class, this::handleGetGroupStructure);

	}

	private CompletableFuture<Optional<GroupLayer>> handleGetGroupStructure (GetGroupStructure getGroupStructure) {
		log.debug ("handleListLayergroups");
		return f.ask(this.serviceManager, new GetGroupLayer(getGroupStructure.groupId())).thenApply(resp -> {
			if(resp instanceof NotFound) {
				return Optional.empty();
			} else {
				return Optional.of((GroupLayer)resp);
			}
		});
	}

	private CompletableFuture<Page<LayerGroup>> handleListLayergroups () {
		log.debug ("handleListLayergroups");
		return 
			db.query().from(genericLayer)
			.leftJoin(leafLayer).on(genericLayer.id.eq(leafLayer.genericLayerId))
			.where(leafLayer.genericLayerId.isNull())
			.list(new QLayerGroup(
					genericLayer.identification,
					genericLayer.name,
					genericLayer.title, 
					genericLayer.abstractCol,
					genericLayer.published
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
					genericLayer.published
			));		
	}
	
	private CompletableFuture<Response<?>> handlePutLayergroup(LayerGroup theLayergroup) {
		String layergroupId = theLayergroup.id();
		String layergroupName = theLayergroup.name();
		log.debug ("handle update/create layergroup: " + layergroupId);
		
		return db.transactional(tx ->
			// Check if there is another layergroup with the same name
			tx.query().from(genericLayer)
			.where(genericLayer.identification.eq(layergroupId))
			.singleResult(genericLayer.identification)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT
					log.debug("Inserting new layergroup with name: " + layergroupName);
					return tx.insert(genericLayer)
					.set(genericLayer.identification, UUID.randomUUID().toString())
					.set(genericLayer.name, layergroupName)
					.set(genericLayer.title, theLayergroup.title())
					.set(genericLayer.abstractCol, theLayergroup.abstractText())
					.set(genericLayer.published, theLayergroup.published())
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, layergroupName));
				} else {
					// UPDATE
					log.debug("Updating layergroup with name: " + layergroupName);
					return tx.update(genericLayer)
							.set(genericLayer.title, theLayergroup.title())
							.set(genericLayer.abstractCol, theLayergroup.abstractText())
							.set(genericLayer.published, theLayergroup.published())
					.where(genericLayer.identification.eq(layergroupId))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, layergroupName));
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteLayergroup(String layergroupId) {
		log.debug("handleDeleteLayergroup: " + layergroupId);
		return db.transactional(tx -> tx
			.query()
			.from(genericLayer)
			.where(genericLayer.identification.eq(layergroupId))
			.singleResult(genericLayer.id)
			.thenCompose(
				glId -> {
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
								log.debug("delete genericLayer: " + glId.get());
								return tx
									.delete(genericLayer)
									.where(genericLayer.id.eq(glId.get()))
									.execute()
									.thenApply(
										l -> new Response<String>(CrudOperation.DELETE,
											CrudResponse.OK, layergroupId));
							});
						// TODO send ERROR message?
					}));
	}
	
}
