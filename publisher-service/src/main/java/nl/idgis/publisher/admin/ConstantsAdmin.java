package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QConstants.constants;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.Constant;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ConstantsAdmin extends AbstractAdmin {
	
	public ConstantsAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(ConstantsAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		doGet(Constant.class, this::handleGetConstants);
		doPut(Constant.class, this::handlePutConstants);
	}

	private CompletableFuture<Optional<Constant>> handleGetConstants (String constantId) {
		log.debug ("handleGetConstants");
		
		return 
			db.query().from(constants)
			.where(constants.identification.eq(constantId))
			.singleResult(new nl.idgis.publisher.domain.web.QConstant(
					constants.identification,
					constants.contact,
					constants.organization,
					constants.position,
					constants.addressType,
					constants.address,
					constants.city,
					constants.state,
					constants.zipcode,
					constants.country,
					constants.telephone,
					constants.fax,
					constants.email));		
	}
	
	private CompletableFuture<Response<?>> handlePutConstants(Constant theConstants) {
		String constantId = theConstants.id();
		String constantContact = theConstants.contact();
		log.debug ("handle update/create constants: " + constantId);
		
		return db.transactional(tx ->
			// Check if there is another constant with the same id
			tx.query().from(constants)
			.where(constants.identification.eq(constantId))
			.singleResult(constants.identification)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT
					log.debug("Inserting constants with name: " + constantContact);
					return tx.insert(constants)
					.set(constants.identification, UUID.randomUUID().toString())
					.set(constants.contact, constantContact)
					.set(constants.organization, theConstants.organization())
					.set(constants.position, theConstants.position())
					.set(constants.addressType, theConstants.addressType())
					.set(constants.address, theConstants.address())
					.set(constants.city, theConstants.city())
					.set(constants.state, theConstants.state())
					.set(constants.zipcode, theConstants.zipcode())
					.set(constants.country, theConstants.country())
					.set(constants.telephone, theConstants.telephone())
					.set(constants.fax, theConstants.fax())
					.set(constants.email, theConstants.email())
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, constantContact));
				} else {
					// UPDATE
					log.debug("Updating constant with name: " + constantContact);
					return tx.update(constants)
					.set(constants.contact, constantContact)
					.set(constants.organization, theConstants.organization())
					.set(constants.position, theConstants.position())
					.set(constants.addressType, theConstants.addressType())
					.set(constants.address, theConstants.address())
					.set(constants.city, theConstants.city())
					.set(constants.state, theConstants.state())
					.set(constants.zipcode, theConstants.zipcode())
					.set(constants.country, theConstants.country())
					.set(constants.telephone, theConstants.telephone())
					.set(constants.fax, theConstants.fax())
					.set(constants.email, theConstants.email())
					.where(constants.identification.eq(constantId))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, constantContact));
				}
		}));
	}
	
}
