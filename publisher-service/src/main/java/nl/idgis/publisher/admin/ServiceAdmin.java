package nl.idgis.publisher.admin;

import static nl.idgis.publisher.database.QConstants.constants;
import static nl.idgis.publisher.database.QDataset.dataset;
import static nl.idgis.publisher.database.QGenericLayer.genericLayer;
import static nl.idgis.publisher.database.QJob.job;
import static nl.idgis.publisher.database.QLeafLayer.leafLayer;
import static nl.idgis.publisher.database.QService.service;
import static nl.idgis.publisher.database.QServiceJob.serviceJob;
import static nl.idgis.publisher.database.QServiceKeyword.serviceKeyword;
import static nl.idgis.publisher.database.QSourceDataset.sourceDataset;
import static nl.idgis.publisher.database.QSourceDatasetVersion.sourceDatasetVersion;
import static nl.idgis.publisher.service.manager.QServiceStructure.serviceStructure;
import static nl.idgis.publisher.service.manager.QServiceStructure.withServiceStructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import nl.idgis.publisher.database.AsyncSQLQuery;
import nl.idgis.publisher.database.QGenericLayer;
import nl.idgis.publisher.domain.query.ListServiceKeywords;
import nl.idgis.publisher.domain.query.ListServices;
import nl.idgis.publisher.domain.query.PutServiceKeywords;
import nl.idgis.publisher.domain.response.Page;
import nl.idgis.publisher.domain.response.Response;
import nl.idgis.publisher.domain.service.CrudOperation;
import nl.idgis.publisher.domain.service.CrudResponse;
import nl.idgis.publisher.domain.web.QService;
import nl.idgis.publisher.domain.web.Service;
import akka.actor.ActorRef;
import akka.actor.Props;

import com.mysema.query.sql.SQLSubQuery;
import com.mysema.query.support.Expressions;
import com.mysema.query.types.Ops;
import com.mysema.query.types.expr.BooleanExpression;

public class ServiceAdmin extends AbstractAdmin {
	
	protected final static QGenericLayer child = new QGenericLayer("child"), parent = new QGenericLayer("parent");
	
	private final ActorRef serviceManager;
	
	public ServiceAdmin(ActorRef database, ActorRef serviceManager) {
		super(database); 
		
		this.serviceManager = serviceManager;
	}
	
	public static Props props(ActorRef database, ActorRef serviceManager) {
		return Props.create(ServiceAdmin.class, database, serviceManager);
	}

	@Override
	protected void preStartAdmin() {
		doList(Service.class, this::handleListServices);
		doGet(Service.class, this::handleGetService);
		doPut(Service.class, this::handlePutService);
		doDelete(Service.class, this::handleDeleteService);
		
		doQuery(ListServiceKeywords.class, this::handleListServiceKeywords);
		doQuery(PutServiceKeywords.class, this::handlePutServiceKeywords);
		
		doQuery (ListServices.class, this::handleListServicesWithQuery);
	}

	private CompletableFuture<Page<Service>> handleListServices () {
		return handleListServicesWithQuery (new ListServices (null, null, null));
	}

	private BooleanExpression isConfidential () {
		return new SQLSubQuery ()
			.from (serviceStructure)
			.join (leafLayer).on (serviceStructure.childLayerId.eq (leafLayer.genericLayerId))
			.join (dataset).on (leafLayer.datasetId.eq (dataset.id))
			.join (sourceDataset).on (dataset.sourceDatasetId.eq (sourceDataset.id))
			.where (serviceStructure.serviceIdentification.eq (genericLayer.identification))
			.where (Expressions.booleanOperation (Ops.EQ, Expressions.constant (true), new SQLSubQuery ().from (sourceDatasetVersion).where (sourceDatasetVersion.sourceDatasetId.eq (sourceDataset.id)).orderBy (sourceDatasetVersion.createTime.desc ()).limit (1).list (sourceDatasetVersion.confidential)))
			.exists ();
	}
	
	private CompletableFuture<Page<Service>> handleListServicesWithQuery (final ListServices listServices) {
		final AsyncSQLQuery baseQuery = db
				.query()
				.from(service)
				.leftJoin(genericLayer).on(service.genericLayerId.eq(genericLayer.id))
				.leftJoin(constants).on(service.constantsId.eq(constants.id))
				.orderBy (genericLayer.name.asc ());
		
		// Add a filter for the query string:
		if (listServices.getQuery () != null) {
			baseQuery.where (genericLayer.name.containsIgnoreCase (listServices.getQuery ())
					.or (genericLayer.title.containsIgnoreCase (listServices.getQuery ()))
				);
		}
		
		// Add a filter for the published flag:
		if (listServices.getPublished () != null) {
			baseQuery.where (genericLayer.published.eq (listServices.getPublished ()));
		}
		
		final AsyncSQLQuery listQuery = baseQuery.clone ();
		
		singlePage (listQuery, listServices.getPage ());
		
		withServiceStructure (listQuery, parent, child);
		
		return baseQuery
				.count ()
				.thenCompose ((count) -> {
					final Page.Builder<Service> builder = new Page.Builder<> ();
					
					addPageInfo (builder, listServices.getPage (), count);
					
					return listQuery
						.list (new QService(
								genericLayer.identification,
								genericLayer.name,
								genericLayer.title, 
								service.alternateTitle, 
								genericLayer.abstractCol,
								service.metadata,
								genericLayer.published,
								genericLayer.identification,					
								constants.identification,
								isConfidential ()
							))
						.thenApply ((styles) -> {
							builder.addAll (styles.list ());
							return builder.build ();
						});
				});
	}
	
	private CompletableFuture<Optional<Service>> handleGetService (String serviceId) {
		log.debug ("handleGetService: " + serviceId);
		
		
		return 
			withServiceStructure (db.query (), parent, child).from(service)
			.leftJoin(genericLayer).on(service.genericLayerId.eq(genericLayer.id))
			.leftJoin(constants).on(service.constantsId.eq(constants.id))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(new QService(
					genericLayer.identification,
					genericLayer.name,
					genericLayer.title, 
					service.alternateTitle, 
					genericLayer.abstractCol,
					service.metadata,
					genericLayer.published,
					genericLayer.identification,					
					constants.identification,
					isConfidential ()
			));		
	}
	
	private CompletableFuture<Response<?>> handlePutService(Service theService) {
		String serviceId = theService.id();
		String serviceName = theService.name();
		log.debug ("handle update/create service: " + serviceId);
		
		return db.transactional(tx ->
			// Check if there is another service with the same id
			tx.query().from(service)
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(genericLayer.identification)
			.thenCompose(msg -> {
				if (!msg.isPresent()){
					// INSERT generic Layer (rootgroup)
					String newGenericLayerId = UUID.randomUUID().toString();
					return tx.insert(genericLayer)
						.set(genericLayer.identification, newGenericLayerId)
						.set(genericLayer.name, serviceName)
						.set(genericLayer.title, theService.title())
						.set(genericLayer.abstractCol, theService.abstractText())
						.set(genericLayer.published, theService.published())
						.execute()
						.thenCompose(n -> {
							return tx.query().from(genericLayer)
								.where(genericLayer.identification.eq(newGenericLayerId))
								.singleResult(genericLayer.id)
								.thenCompose(glId -> {
									return tx.query().from(constants)
										.singleResult(constants.id)
										.thenCompose(cId -> {
											if (glId.isPresent()){
												// INSERT service
												log.debug("Inserting new service with name: " + serviceName + ", ident: "
														+ newGenericLayerId);
												return tx.insert(service)
													.set(service.alternateTitle, theService.alternateTitle())
													.set(service.metadata, theService.metadata())
													.set(service.genericLayerId, glId.get())
													.set(service.constantsId, cId.get())
													.execute()
													.thenApply(l -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, newGenericLayerId));
											} else {
												//ERROR
												return f.successful(new Response<String>(CrudOperation.CREATE, CrudResponse.NOK, serviceName));
											}
										});
							});
						});
				} else {
					return tx.query().from(genericLayer)
						.where(genericLayer.identification.eq(theService.genericLayerId()))
						.singleResult(genericLayer.id)
						.thenCompose(glId -> {
							if (glId.isPresent()){
								// UPDATE generic Layer (rootgroup)
								return tx.update(genericLayer)
									.set(genericLayer.name, serviceName)
									.set(genericLayer.title, theService.title())
									.set(genericLayer.abstractCol, theService.abstractText())
									.set(genericLayer.published, theService.published())
									.where(genericLayer.id.eq(glId.get()))
									.execute()
									.thenCompose(n -> {
										// UPDATE service
										log.debug("Updating service with name: " + serviceName);
										return tx.update(service)
											.set(service.alternateTitle, theService.alternateTitle())
											.set(service.metadata, theService.metadata())
											.where(service.genericLayerId.eq(glId.get()))
											.execute()
											.thenApply(l -> new Response<String>(CrudOperation.UPDATE, CrudResponse.OK, serviceId));
									});
							} else {
								//ERROR
								return f.successful(new Response<String>(CrudOperation.UPDATE, CrudResponse.NOK, serviceName));
							}
						});
				}
		}));
	}

	private CompletableFuture<Response<?>> handleDeleteService(String serviceId) {
		log.debug ("handleDeleteService: " + serviceId);
		return db.transactional(tx ->
				tx.query().from(service)
				.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
				.where(genericLayer.identification.eq(serviceId))
				.singleResult(service.id)
				.thenCompose(svId -> {
					log.debug("delete jobs for service id: {}", svId.get());
					return tx.delete(job)
						.where(new SQLSubQuery().from(serviceJob)
								.where(serviceJob.serviceId.eq(svId.get())
									.and(job.id.eq(serviceJob.jobId)))
								.exists())
						.execute()
						.thenCompose(jobs -> {
							log.debug("delete service id: {}", svId.get());
							return tx.delete(service)
								.where(service.id.eq(svId.get()))
								.execute()
								.thenCompose(n -> {
									log.debug("delete generic layer id: {}", serviceId);
									return tx.delete(genericLayer)
										.where(genericLayer.identification.eq(serviceId))
										.execute()
										.thenApply(l -> 
											new Response<String>(
												CrudOperation.DELETE, 
												CrudResponse.OK, 
												serviceId));
									});
							});
					}));

	}
	
	
	
	private CompletableFuture<List<String>> handleListServiceKeywords(ListServiceKeywords listServiceKeywords) {
		final String serviceId = listServiceKeywords.serviceId();
		log.debug ("handleListServiceKeywords: " + serviceId);
		
		return db.transactional(
			tx ->
			tx.query().from(service)
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(service.id)
			.thenCompose(
				svId -> {
				log.debug("service id: " + svId.get());
				return tx.query()
					.from(serviceKeyword)
					.where(serviceKeyword.serviceId.eq(svId.get()))
					.list(serviceKeyword.keyword)
					.thenApply(resp -> resp.list());
				})
		);
	}
	
	private CompletableFuture<Response<?>> handlePutServiceKeywords(PutServiceKeywords putServiceKeywords) {
		final String serviceId = putServiceKeywords.serviceId();
		final List<String> serviceKeywords = putServiceKeywords.keywordList();
		
		log.debug ("handlePutServiceKeywords: " + serviceId);
		return db.transactional(
			tx ->
			tx.query().from(service)
			.join(genericLayer).on(genericLayer.id.eq(service.genericLayerId))
			.where(genericLayer.identification.eq(serviceId))
			.singleResult(service.id)
			.thenCompose(
				svId -> {
				// A. delete the existing keywords of this service
				log.debug("service id: " + svId.get());
				return tx.delete(serviceKeyword)
					.where(serviceKeyword.serviceId.eq(svId.get()))
					.execute()
					.thenCompose(
						skNr -> {
						// B. insert items of service keywords	
						return f.sequence(
							serviceKeywords.stream()
							    .map(name -> 
							    	tx.insert(serviceKeyword)
						            .set(serviceKeyword.serviceId, svId.get()) 
				            		.set(serviceKeyword.keyword, name)
						            .execute())
							    .collect(Collectors.toList())).thenApply(whatever ->
							        new Response<String>(CrudOperation.CREATE,
							                CrudResponse.OK, serviceId));
						});
//					.thenCompose(
//						n -> {
//						log.debug("delete keywords: #" + n);
//						return tx
//							.insert(serviceKeyword)							
//							.set(serviceKeyword.serviceId, svId.get())
//							.set(serviceKeyword.keyword, "abcdef")
//							.execute()
//							.thenApply(resp -> new Response<String>(CrudOperation.CREATE, CrudResponse.OK, serviceId));
//						});
				})
		);		
		
	}	
}
