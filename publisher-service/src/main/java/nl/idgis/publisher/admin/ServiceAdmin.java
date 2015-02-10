package nl.idgis.publisher.admin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mysema.query.types.ConstantImpl;

import static nl.idgis.publisher.database.QService.service;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.QService;
import nl.idgis.publisher.domain.web.Service;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ServiceAdmin extends AbstractAdmin {
	
	public ServiceAdmin(ActorRef database) {
		super(database); 
	}
	
	public static Props props(ActorRef database) {
		return Props.create(ServiceAdmin.class, database);
	}

	@Override
	protected void preStartAdmin() {
		addList(Service.class, this::handleListServices);
		addGet(Service.class, this::handleGetService);
		addPut(Service.class, this::handlePutService);
		addDelete(Service.class, this::handleDeleteService);
	}

	private CompletableFuture<Page<Service>> handleListServices () {
		log.debug ("handleListServices");
		
		return 
			db.query().from(service)
			.list(new QService(
					service.identification,
					service.name,
					service.title, 
					service.alternateTitle, 
					service.abstractCol,
					service.metadata,
					service.keywords,
					service.watermark,
					ConstantImpl.create(false)
				))
			.thenApply(this::toPage);
	}

	
	private CompletableFuture<Optional<Service>> handleGetService (String serviceId) {
		log.debug ("handleGetService: " + serviceId);
		
		return 
			db.query().from(service)
			.where(service.identification.eq(serviceId))
			.singleResult(new QService(
					service.identification,
					service.name,
					service.title, 
					service.alternateTitle, 
					service.abstractCol,
					service.metadata,
					service.keywords,
					service.watermark,
					ConstantImpl.create(false)
			));		
	}
	
	private CompletableFuture<Response<?>> handlePutService(Service theService) {
		String serviceId = theService.id();
		String serviceName = theService.name();
		log.debug ("handle update/create service: " + serviceId);
		
		return db.transactional(tx ->
			// Check if there is another service with the same name
			tx.query().from(service)
			.where(service.identification.eq(serviceId))
			.singleResult(service.identification)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT
					log.debug("Inserting new service with name: " + serviceName);
					return tx.insert(service)
					.set(service.identification, UUID.randomUUID().toString())
					.set(service.name, serviceName)
					.set(service.title, theService.title())
					.set(service.alternateTitle, theService.alternateTitle())
					.set(service.abstractCol, theService.abstractText())
					.set(service.metadata, theService.metadata())
					.set(service.keywords, theService.keywords())
					.set(service.watermark, theService.watermark())
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, serviceName));
				} else {
					// UPDATE
					log.debug("Updating service with name: " + serviceName);
					return tx.update(service)
							.set(service.title, theService.title())
							.set(service.alternateTitle, theService.alternateTitle())
							.set(service.abstractCol, theService.abstractText())
							.set(service.metadata, theService.metadata())
							.set(service.keywords, theService.keywords())
							.set(service.watermark, theService.watermark())
					.where(service.identification.eq(serviceId))
					.execute()
					.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, serviceName));
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteService(String serviceId) {
		log.debug ("handleDeleteService: " + serviceId);
		return db.delete(service)
			.where(service.identification.eq(serviceId))
			.execute()
			.thenApply(l -> new Response<String>(CrudOperation.DELETE, CrudResponse.OK, serviceId));
	}
	
}
